package com.popIt.pop_it.domain.contract.service;

import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 계약 서명이 거부되어야 하는 실패 케이스(내용 변조 / 본인인증 미완료 / 서명 순서 위반)를 검증한다.
@SpringBootTest
@Transactional
public class ContractSignatureFailureTest {

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

    // 실제 PortOne/S3 연동 없이 계약 서명 로직만 검증하기 위해 외부 연동 지점을 목으로 대체한다.
    @MockitoBean
    private IdentityVerificationService identityVerificationService;
    @MockitoBean
    private S3ObjectHasher s3ObjectHasher;

    private static final String HOST_CI_HASH = "host-ci-hash";
    private static final String GUEST_CI_HASH = "guest-ci-hash";
    private static final String IMAGE_HASH = "signature-image-hash";

    private User host;
    private User guest;
    private Long hostId;
    private Long guestId;
    private Long reservationId;

    @BeforeEach
    void setUp() {
        host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("contract-failure-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("contract-failure-guest-uid")
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());
        guestId = guest.getUserId();

        Space space = spaceRepository.save(Space.builder()
                .buildingName("계약 실패 테스트용 빌딩")
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
                .description("계약 서명 실패 테스트용 공간")
                .hostId(hostId)
                .build());

        LocalDate reservationStartDate = LocalDate.now().plusDays(10);
        LocalDate reservationEndDate = LocalDate.now().plusDays(12);
        String usagePurpose = "계약 서명 실패 테스트";

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .status(ReservationStatus.APPROVED) // 계약 단계 전제 상태
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

        // 실제 생성 경로(contentHash 계산 포함)를 그대로 태워서, verifyContentIntegrity가
        // 통과할 수 있는 진짜 해시가 저장되도록 한다.
        contractService.createPendingContract(reservation);

        // 본인인증은 완료된 상태를 기본값으로 두고, 이미지 해시는 S3를 실제로 호출하지 않도록 고정값을 반환
        when(identityVerificationService.getVerifiedCiHash(any(User.class)))
                .thenAnswer(invocation -> {
                    User signer = invocation.getArgument(0);
                    return Optional.of(signer.getUserId().equals(hostId) ? HOST_CI_HASH : GUEST_CI_HASH);
                });
        when(s3ObjectHasher.hash(anyString(), anyString(), anyString())).thenReturn(IMAGE_HASH);
    }

    @Test
    void 계약_체결_후_내용이_변조되면_서명_시_예외가_발생한다() {
        // 전제: contentHash가 가리키는 스냅샷과 실제 컬럼 값이 어긋나도록, 체결 이후에만 가능한 방식(DB 직접 조작 등)으로
        // Contract의 임대료를 변경한다. 엔티티에는 별도의 setter가 없어 위변조 상황을 재현하기 위해 리플렉션을 사용한다.
        Contract contract = contractRepository.findByReservation_Id(reservationId).orElseThrow();
        ReflectionTestUtils.setField(contract, "rentalFee", contract.getRentalFee() + 1);

        assertThatThrownBy(() -> contractService.signature(
                host, reservationId, new ContractReqDTO.ContractSignatureReq("https://signature.example.com/host.png")))
                .isInstanceOf(ContractException.class)
                .satisfies(e -> assertThat(((ContractException) e).getErrorCode())
                        .isEqualTo(ContractErrorCode.CONTRACT_CONTENT_TAMPERED));

        // 응답: 변조가 감지되면 서명 정보가 반영되지 않고 그대로 남아있어야 한다
        Contract untouched = contractRepository.findByReservation_Id(reservationId).orElseThrow();
        assertThat(untouched.getHostSignatureUrl()).isNull();
        assertThat(untouched.getHostSignedAt()).isNull();
    }

    @Test
    void 본인인증이_완료되지_않은_사용자가_서명하면_예외가_발생한다() {
        // 전제: 호스트의 본인인증이 아직 완료되지 않아 ciHash를 조회할 수 없는 상태
        when(identityVerificationService.getVerifiedCiHash(host)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.signature(
                host, reservationId, new ContractReqDTO.ContractSignatureReq("https://signature.example.com/host.png")))
                .isInstanceOf(ContractException.class)
                .satisfies(e -> assertThat(((ContractException) e).getErrorCode())
                        .isEqualTo(ContractErrorCode.CONTRACT_SIGNER_NOT_VERIFIED));

        // 응답: 본인인증 실패로 서명 자체가 시도되지 않았으므로 상태가 그대로여야 한다
        Contract untouched = contractRepository.findByReservation_Id(reservationId).orElseThrow();
        assertThat(untouched.getHostSignatureUrl()).isNull();
        assertThat(untouched.getHostSignedAt()).isNull();
    }

    @Test
    void 호스트보다_먼저_게스트가_서명하면_순서_위반_예외가_발생한다() {
        // 전제: 아직 호스트가 서명하지 않아 계약이 HOST_SIGNATURE_PENDING 상태로 남아있음
        assertThatThrownBy(() -> contractService.signature(
                guest, reservationId, new ContractReqDTO.ContractSignatureReq("https://signature.example.com/guest.png")))
                .isInstanceOf(ContractException.class)
                .satisfies(e -> assertThat(((ContractException) e).getErrorCode())
                        .isEqualTo(ContractErrorCode.CONTRACT_NOT_GUEST_SIGNATURE_ORDER));

        // 응답: 순서 위반으로 게스트 서명 정보가 반영되지 않아야 한다
        Contract untouched = contractRepository.findByReservation_Id(reservationId).orElseThrow();
        assertThat(untouched.getGuestSignatureUrl()).isNull();
        assertThat(untouched.getGuestSignedAt()).isNull();
    }

    @Test
    void 이미_모두_서명된_계약에_다시_서명하면_예외가_발생한다() {
        // 전제: 호스트, 게스트 모두 정상적으로 서명을 마쳐 PENDING_PAYMENT 상태가 된 계약
        contractService.signature(host, reservationId, new ContractReqDTO.ContractSignatureReq("https://signature.example.com/host.png"));
        contractService.signature(guest, reservationId, new ContractReqDTO.ContractSignatureReq("https://signature.example.com/guest.png"));

        // 게스트가 이미 완료된 계약에 순서를 어기고 재요청하는 경우
        assertThatThrownBy(() -> contractService.signature(
                guest, reservationId, new ContractReqDTO.ContractSignatureReq("https://signature.example.com/guest-retry.png")))
                .isInstanceOf(ContractException.class)
                .satisfies(e -> assertThat(((ContractException) e).getErrorCode())
                        .isEqualTo(ContractErrorCode.CONTRACT_ALREADY_ALL_SIGNED));
    }
}
