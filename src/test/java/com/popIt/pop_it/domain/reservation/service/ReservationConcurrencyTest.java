package com.popIt.pop_it.domain.reservation.service;

import com.popIt.pop_it.domain.identity_verification.repository.IdentityVerificationRepository;
import com.popIt.pop_it.domain.reservation.dto.ReservationReqDTO;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.*;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ReservationConcurrencyTest {

    private static final int THREAD_COUNT = 10;

    @Autowired
    private ReservationCommandService reservationCommandService;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private IdentityVerificationRepository identityVerificationRepository;

    private Long spaceId;
    private List<Long> guestIds;

    @BeforeEach
    void setUp() {
        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());

        Space space = spaceRepository.save(Space.builder()
                .buildingName("테스트빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .addressDetail("101동 101호")
                .dong("합정동")
                .deposit(1_000_000L)
                .pricePerDay(100_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("동시성 테스트용 공간")
                .hostId(host.getUserId())
                .build());
        spaceId = space.getId();

        guestIds = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> userRepository.save(User.builder()
                        .socialProvider(SocialProvider.KAKAO)
                        .socialUid("guest-uid-" + i)
                        .nickname("guest" + i)
                        .currentMode(UserMode.GUEST)
                        .build()).getUserId())
                .toList();
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        spaceRepository.deleteAll();
        identityVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 동시에_같은_기간으로_예약요청하면_한_건만_성공한다() throws InterruptedException {
        ReservationReqDTO.ReservationCreateReq request = new ReservationReqDTO.ReservationCreateReq(
                spaceId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), "동시성 테스트"
        );

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        AtomicInteger unexpectedCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            Long guestId = guestIds.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 출발하도록 대기
                    reservationCommandService.createReservation(guestId, request);
                    successCount.incrementAndGet();
                } catch (ProjectException e) {
                    if (e.getErrorCode() == ReservationErrorCode.RESERVATION_ALREADY_TAKEN
                            || e.getErrorCode() == ReservationErrorCode.RESERVATION_SPACE_LOCKED) {
                        // ALREADY_TAKEN: 락 획득 후 겹치는 예약 발견 / SPACE_LOCKED: 락 즉시실패(lock.timeout=0)로 인한 경합 실패
                        conflictCount.incrementAndGet();
                    } else {
                        unexpectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    unexpectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean completedInTime;
        try {
            startLatch.countDown(); // 출발 신호
            completedInTime = doneLatch.await(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        assertThat(completedInTime).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(THREAD_COUNT - 1);
        assertThat(unexpectedCount.get()).isZero();
        assertThat(reservationRepository.count()).isEqualTo(1);
    }



}
