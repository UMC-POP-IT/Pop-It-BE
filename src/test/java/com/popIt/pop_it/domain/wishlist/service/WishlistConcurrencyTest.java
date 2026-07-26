package com.popIt.pop_it.domain.wishlist.service;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.*;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
public class WishlistConcurrencyTest {

    private static final int THREAD_COUNT = 10;

    @Autowired
    private WishlistService wishlistService;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WishlistRepository wishlistRepository;

    private Long userId;
    private Long spaceId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("wish-concurrency-uid")
                .nickname("wish-concurrency")
                .currentMode(UserMode.GUEST)
                .build());
        userId = user.getUserId();

        Space space = spaceRepository.save(Space.builder()
                .buildingName("동시성 테스트용 빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .addressDetail("3층 301호")
                .deposit(1_000_000L)
                .pricePerDay(100_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("찜 동시성 테스트용 공간")
                .hostId(user.getUserId())
                .build());
        spaceId = space.getId();
    }

    @AfterEach
    void tearDown() {
        wishlistRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 사용자가 같은 공간을 동시에 여러 번 찜 토글해도 중복 저장/예외 누출이 없다")
    void 동시에_같은_공간을_토글해도_중복행이_생기지_않는다() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger();   // 정상 처리(등록/해제 무관)
        AtomicInteger unexpectedCount = new AtomicInteger(); // 예상치 못한 예외(500 후보)

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 출발하도록 대기
                    wishlistService.toggle(userId, spaceId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 유니크 충돌(DataIntegrityViolationException)은 서비스에서 잡아 정상 처리하므로
                    // 여기까지 올라오는 예외는 모두 '예상치 못한' 것으로 집계한다
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
        // 핵심 불변식 1: 예상치 못한 예외(500)가 하나도 없어야 한다 → 독립 트랜잭션 + catch 방어가 동작
        assertThat(unexpectedCount.get()).isZero();
        // 모든 요청이 예외 없이 완료되어야 한다
        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        // 핵심 불변식 2: 토글 결과(0 또는 1)와 무관하게, (user, space) 중복 row는 절대 생기지 않는다
        assertThat(wishlistRepository.countBySpaceId(spaceId)).isLessThanOrEqualTo(1);
    }
}
