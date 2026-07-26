package com.popIt.pop_it.domain.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// V1__add_pending_payment_unique_index.sql이 "같은 contract_id에 PENDING 결제는 최대 1건"이라는
// 동시성 제약을 실제 DB(MySQL) 레벨에서 강제하는지 검증한다.
// H2는 이 마이그레이션의 함수형 인덱스(CASE WHEN ...) 문법을 지원하지 않고,
// application.yml에서 로컬/테스트는 spring.flyway.enabled=false로 꺼두었기 때문에
// 실제 제약 검증은 Testcontainers MySQL에서만 가능하다.
// (@ServiceConnection이 datasource만 컨테이너로 갈아끼우고, 나머지 설정은 test/application.yml을 그대로 쓴다)

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true) // 도커 실행 후 테스트 실행
class PaymentPendingUniqueIndexTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private DataSource dataSource;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    // test/application.yml의 hibernate.ddl-auto(create-drop)가 컨텍스트 기동 시 테이블을 먼저 만들고,
    // 그 위에 V1 마이그레이션(인덱스 추가)을 얹어야 실제 운영 환경(테이블은 이미 존재 + 증분 마이그레이션)과 같은 순서가 된다.
    // 컨텍스트가 테스트 클래스 전체에서 캐시되므로 한 번만 실행되면 충분하다.
    private static boolean indexMigrated = false;

    @BeforeEach
    void migratePendingUniqueIndexOnce() {
        if (!indexMigrated) {
            // hibernate.ddl-auto(create-drop)가 먼저 테이블을 다 만들어버려서 스키마가 비어있지 않은 상태로 Flyway를 만난다.
            // baselineVersion을 V1보다 낮은 0으로 잡아야 baseline 처리 후에도 V1(인덱스 추가)이 스킵되지 않고 실제로 실행된다.
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load();
            flyway.migrate();
            indexMigrated = true;
        }
    }

    private Contract newContract() {
        String suffix = UUID.randomUUID().toString();

        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("index-test-host-" + suffix)
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());

        User guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("index-test-guest-" + suffix)
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());

        Space space = spaceRepository.save(Space.builder()
                .buildingName("유니크 인덱스 테스트용 빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .addressDetail("101호")
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
                .description("유니크 인덱스 테스트용 공간")
                .hostId(host.getUserId())
                .build());

        LocalDate reservationStartDate = LocalDate.now().plusDays(10);
        LocalDate reservationEndDate = LocalDate.now().plusDays(12);
        String usagePurpose = "유니크 인덱스 테스트";

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

        return contractRepository.save(Contract.builder()
                .status(ContractStatus.COMPLETED)
                .hostId(host.getUserId())
                .guestId(guest.getUserId())
                .spaceId(space.getId())
                .spaceName(space.getBuildingName())
                .roadAddress(space.getRoadAddress())
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
    }

    private Payment pendingPaymentOf(Contract contract, String suffix) {
        return Payment.builder()
                .status(PaymentStatus.PENDING)
                .orderId("ORDER-" + suffix)
                .idempotencyKey("IDEMPOTENCY-" + suffix)
                .contract(contract)
                .build();
    }

    @Test
    void 같은_계약에_PENDING_결제가_두_건_이상_존재하면_유니크_인덱스_위반으로_실패한다() {
        Contract contract = newContract();
        String suffix = UUID.randomUUID().toString();

        paymentRepository.saveAndFlush(pendingPaymentOf(contract, "first-" + suffix));

        assertThatThrownBy(() ->
                paymentRepository.saveAndFlush(pendingPaymentOf(contract, "second-" + suffix)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void PENDING_결제가_다른_상태로_전환되면_같은_계약에_새_PENDING_결제를_다시_만들_수_있다() {
        Contract contract = newContract();
        String suffix = UUID.randomUUID().toString();

        Payment first = paymentRepository.saveAndFlush(pendingPaymentOf(contract, "first-" + suffix));
        first.markAsPaid("paymentKey-" + suffix, PaymentMethod.CARD, LocalDateTime.now());
        paymentRepository.saveAndFlush(first);

        Payment second = paymentRepository.saveAndFlush(pendingPaymentOf(contract, "second-" + suffix));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(paymentRepository.findByContractIdAndStatus(contract.getId(), PaymentStatus.PENDING))
                .map(Payment::getId)
                .contains(second.getId());
    }

    @Test
    void 서로_다른_계약이면_각각_PENDING_결제를_동시에_가질_수_있다() {
        Contract contractA = newContract();
        Contract contractB = newContract();
        String suffix = UUID.randomUUID().toString();

        paymentRepository.saveAndFlush(pendingPaymentOf(contractA, "a-" + suffix));

        assertThatCode(() ->
                paymentRepository.saveAndFlush(pendingPaymentOf(contractB, "b-" + suffix)))
                .doesNotThrowAnyException();
    }
}
