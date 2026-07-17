package com.popIt.pop_it.domain.contract.service;

import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ContractSignatureTest {

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

    private User host;
    private User guest;
    private Long hostId;
    private Long guestId;
    private Long reservationId;

    @BeforeEach
    void setUp() {
        host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("contract-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("contract-guest-uid")
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());
        guestId = guest.getUserId();

        Space space = spaceRepository.save(Space.builder()
                .buildingName("계약 테스트용 빌딩")
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
                .description("계약 서명 테스트용 공간")
                .hostId(hostId)
                .build());

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .status(ReservationStatus.APPROVED) // 계약 단계 전제 상태
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .usagePurpose("계약 서명 테스트")
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .space(space)
                .user(guest)
                .build());
        reservationId = reservation.getId();

        contractRepository.save(Contract.builder()
                .status(ContractStatus.HOST_SIGNATURE_PENDING)
                .reservation(reservation)
                .build());
    }

    @Test
    @Order(1)
    void 호스트_계약_서명() {
        String signatureUrl = "https://signature.example.com/host.png";

        ContractResDTO.SignatureRes result = contractService.signature(
                host, reservationId, new ContractReqDTO.SignatureReq(signatureUrl)
        );

        // 응답: 게스트 서명 대기 상태로 전이, 아직 둘 다 서명된 건 아님
        assertThat(result.contractStatus()).isEqualTo(ContractStatus.GUEST_SIGNATURE_PENDING);
        assertThat(result.bothSigned()).isFalse();

        // 실제 DB에도 호스트 서명 정보가 반영됐는지 확인
        Contract contract = contractRepository.findByReservation_Id(reservationId).orElseThrow();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.GUEST_SIGNATURE_PENDING);
        assertThat(contract.getHostSignatureUrl()).isEqualTo(signatureUrl);
        assertThat(contract.getHostSignedAt()).isNotNull();
        assertThat(contract.getGuestSignatureUrl()).isNull();
    }

    @Test
    @Order(2)
    void 게스트_계약_서명() {
        String hostSignatureUrl = "https://signature.example.com/host.png";
        String guestSignatureUrl = "https://signature.example.com/guest.png";

        // 전제: 호스트가 먼저 서명해서 GUEST_SIGNATURE_PENDING 상태로 만들어둠
        contractService.signature(host, reservationId, new ContractReqDTO.SignatureReq(hostSignatureUrl));

        ContractResDTO.SignatureRes result = contractService.signature(
                guest, reservationId, new ContractReqDTO.SignatureReq(guestSignatureUrl)
        );

        // 응답: 완료 상태로 전이, 둘 다 서명됨
        assertThat(result.contractStatus()).isEqualTo(ContractStatus.COMPLETED);
        assertThat(result.bothSigned()).isTrue();

        // 실제 DB에도 게스트 서명 정보가 반영됐는지 확인
        Contract contract = contractRepository.findByReservation_Id(reservationId).orElseThrow();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.COMPLETED);
        assertThat(contract.getHostSignatureUrl()).isEqualTo(hostSignatureUrl);
        assertThat(contract.getGuestSignatureUrl()).isEqualTo(guestSignatureUrl);
        assertThat(contract.getGuestSignedAt()).isNotNull();
    }
}
