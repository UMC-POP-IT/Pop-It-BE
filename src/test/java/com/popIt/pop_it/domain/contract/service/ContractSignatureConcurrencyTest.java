package com.popIt.pop_it.domain.contract.service;

import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.exception.ContractException;
import com.popIt.pop_it.domain.contract.exception.code.ContractErrorCode;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.identity_verification.service.IdentityVerificationService;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.*;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.util.S3ObjectHasher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// dab988d(동시성 제어 리뷰 반영)에서 추가한 Contract.@Version + saveAndFlush()가
// 실제 트랜잭션/DB 레벨에서 의도대로 동작하는지 검증한다.
// 여러 스레드가 각자 독립된 트랜잭션(커넥션)을 가져야 하므로 클래스 레벨 @Transactional을 쓰지 않고,
// 각 테스트가 만든 데이터는 @AfterEach에서 직접 정리한다.
@SpringBootTest
class ContractSignatureConcurrencyTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private ContractService contractService;

    // 실제 PortOne/S3 연동 없이 동시성 로직만 검증하기 위해 외부 연동 지점을 목으로 대체한다.
    @MockitoBean
    private IdentityVerificationService identityVerificationService;
    @MockitoBean
    private S3ObjectHasher s3ObjectHasher;

    private Long hostId;
    private Long guestId;
    private Long spaceId;
    private Long reservationId;
    private Long contractId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("concurrency-host-" + suffix)
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        User guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("concurrency-guest-" + suffix)
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());
        guestId = guest.getUserId();

        Space space = spaceRepository.save(Space.builder()
                .buildingName("동시성 테스트용 빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .addressDetail("101동 101호")
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
                .hostId(hostId)
                .build());
        spaceId = space.getId();

        LocalDate reservationStartDate = LocalDate.now().plusDays(10);
        LocalDate reservationEndDate = LocalDate.now().plusDays(12);
        String usagePurpose = "동시성 테스트";

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .status(ReservationStatus.APPROVED)
                .startDate(reservationStartDate)
                .endDate(reservationEndDate)
                .usagePurpose(usagePurpose)
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .space(space)
                .user(guest)
                .build());
        reservationId = reservation.getId();

        Contract contract = contractRepository.save(Contract.builder()
                .status(ContractStatus.HOST_SIGNATURE_PENDING)
                .hostId(hostId)
                .guestId(guestId)
                .startDate(reservationStartDate)
                .endDate(reservationEndDate)
                .usagePurpose(usagePurpose)
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .contentHash("test-content-hash")
                .reservation(reservation)
                .build());
        contractId = contract.getId();

        // 본인인증/이미지 해시는 이 테스트의 관심사가 아니므로 항상 성공하는 값으로 고정
        when(identityVerificationService.getVerifiedCiHash(any(User.class))).thenReturn(Optional.of("ci-hash"));
        when(s3ObjectHasher.hash(anyString(), anyString(), anyString())).thenReturn("image-hash");
    }

    @AfterEach
    void tearDown() {
        // 파일 기반 H2를 공유해서 쓰므로, 실패 여부와 무관하게 이번 테스트가 만든 행은 반드시 정리한다.
        safeDelete(() -> contractRepository.deleteById(contractId));
        safeDelete(() -> reservationRepository.deleteById(reservationId));
        safeDelete(() -> spaceRepository.deleteById(spaceId));
        safeDelete(() -> userRepository.deleteById(hostId));
        safeDelete(() -> userRepository.deleteById(guestId));
    }

    private void safeDelete(Runnable delete) {
        try {
            delete.run();
        } catch (Exception ignored) {
            // setUp이 중간에 실패해 일부만 만들어진 경우 등, 정리 자체의 실패는 무시한다.
        }
    }

    @Test
    void 오래된_버전으로_저장을_시도하면_낙관적_락_충돌_예외가_발생한다() {
        Contract managed = contractRepository.findById(contractId).orElseThrow();
        assertThat(managed.getVersion()).isEqualTo(0L);

        // 다른 트랜잭션이 먼저 호스트 서명을 커밋해 버전이 0 -> 1로 올라간 상황을 재현
        managed.signByHost(ContractStatus.GUEST_SIGNATURE_PENDING,
                "https://signature.example.com/first.png", "ci-hash", "image-hash");
        contractRepository.saveAndFlush(managed);

        // 그 커밋을 보지 못한 채 여전히 version=0인 "오래된" 사본으로 뒤늦게 저장을 시도
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        Contract stale = Contract.builder()
                .id(contractId)
                .status(ContractStatus.HOST_SIGNATURE_PENDING)
                .hostId(hostId)
                .guestId(guestId)
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .usagePurpose(reservation.getUsagePurpose())
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .contentHash("test-content-hash")
                .reservation(reservation)
                .version(0L)
                .build();
        stale.signByHost(ContractStatus.GUEST_SIGNATURE_PENDING,
                "https://signature.example.com/stale.png", "ci-hash", "image-hash");

        assertThatThrownBy(() -> contractRepository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void 같은_계약에_동시에_두_번_서명_요청이_들어오면_하나만_성공하고_계약은_이중반영되지_않는다() throws InterruptedException {
        User host = userRepository.findById(hostId).orElseThrow();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        Callable<ContractResDTO.SignatureRes> attempt = () -> {
            // 두 스레드가 최대한 같은 순간에 signature()를 호출하도록 동기화해 레이스를 유도
            barrier.await(5, TimeUnit.SECONDS);
            return contractService.signature(
                    host, reservationId,
                    new ContractReqDTO.SignatureReq("https://signature.example.com/host.png"));
        };

        List<Future<ContractResDTO.SignatureRes>> futures = executor.invokeAll(List.of(attempt, attempt));
        executor.shutdown();

        int successCount = 0;
        int conflictCount = 0;
        for (Future<ContractResDTO.SignatureRes> future : futures) {
            try {
                ContractResDTO.SignatureRes result = future.get();
                assertThat(result.contractStatus()).isEqualTo(ContractStatus.GUEST_SIGNATURE_PENDING);
                successCount++;
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(ContractException.class);
                ContractException contractException = (ContractException) e.getCause();
                // 늦게 도착한 요청은, 상대방의 커밋을 이미 봤다면 "이미 서명됨"(비즈니스 상태 체크)을,
                // 같은 버전을 읽은 채 저장 시점에야 걸렸다면 "동시 수정"(낙관적 락)을 받는다.
                // 어느 쪽이 걸리는지는 스레드 스케줄링에 따라 달라지므로 둘 다 정상 결과로 허용한다.
                assertThat(contractException.getErrorCode()).isIn(
                        ContractErrorCode.CONTRACT_ALREADY_HOST_SIGNED,
                        ContractErrorCode.CONTRACT_CONCURRENT_MODIFICATION);
                conflictCount++;
            }
        }

        // 정확히 하나만 성공해야 한다 - 둘 다 성공하거나 둘 다 실패하면 동시성 제어가 깨진 것
        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        Contract finalContract = contractRepository.findById(contractId).orElseThrow();
        assertThat(finalContract.getStatus()).isEqualTo(ContractStatus.GUEST_SIGNATURE_PENDING);
        assertThat(finalContract.getVersion()).isEqualTo(1L); // 성공한 커밋 한 번만 버전을 올렸는지 확인
    }
}
