package com.popIt.pop_it.domain.reservation.service;

import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ReservationOptimisticLockTest {
    private static final int THREAD_COUNT = 2;

    @Autowired
    private ReservationCommandService reservationCommandService;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private UserRepository userRepository;

    private Long reservationId;
    private Long hostId;

    @BeforeEach
    void setUp() {
        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("lock-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        User guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("lock-guest-uid")
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());

        Space space = spaceRepository.save(Space.builder()
                .buildingName("테스트용빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .deposit(1_000_000L)
                .pricePerDay(100_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("낙관적 락 테스트용 공간")
                .hostId(hostId)
                .build());

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .status(ReservationStatus.PENDING_APPROVAL)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .usagePurpose("낙관적 락 테스트")
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .totalPrice(1_210_000L)
                .space(space)
                .user(guest)
                .build());
        reservationId = reservation.getId();
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    //스케줄러와 호스트 경합 상황은 테스트로 재현하기 어려워서 대신 호스트가 같은 예약 동시 승인 시도하는 테스트로 구현
    void 같은_예약을_동시에_승인시도하면_한_건만_성공한다() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        AtomicInteger unexpectedCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    reservationCommandService.approveReservation(reservationId, hostId);
                    successCount.incrementAndGet();
                } catch (ProjectException e) {
                    if (e.getErrorCode() == ReservationErrorCode.RESERVATION_CONCURRENT_MODIFICATION
                            || e.getErrorCode() == ReservationErrorCode.RESERVATION_NOT_MODIFIABLE) {
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

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
        assertThat(unexpectedCount.get()).isZero();

        Reservation result = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }
}
