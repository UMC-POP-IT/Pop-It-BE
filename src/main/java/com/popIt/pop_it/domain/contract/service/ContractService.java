package com.popIt.pop_it.domain.contract.service;

import com.popIt.pop_it.domain.contract.converter.ContractConverter;
import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.exception.ContractException;
import com.popIt.pop_it.domain.contract.exception.code.ContractErrorCode;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.identity_verification.service.IdentityVerificationService;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.exception.ReservationException;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.upload.enums.UploadType;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.global.config.AwsProperties;
import com.popIt.pop_it.global.util.CryptoService;
import com.popIt.pop_it.global.util.S3ObjectHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private static final String FIELD_SEPARATOR = "\u001F"; // usagePurpose 등 자유 텍스트와 구분자 충돌 방지용 제어문자

    private final ContractRepository contractRepository;
    private final ReservationRepository reservationRepository;
    private final IdentityVerificationService identityVerificationService;
    private final S3ObjectHasher s3ObjectHasher;
    private final AwsProperties awsProperties;
    private final CryptoService cryptoService;

    // 계약 생성
    @Transactional
    public void createPendingContract(Reservation reservation) {
        if (contractRepository.findByReservation_Id(reservation.getId()).isPresent()) {
            return; // 이미 계약이 생성돼 있으면 아무 것도 안 하고 성공 처리
        }

        contractRepository.save(ContractConverter.toPendingContract(reservation, buildContentHash(reservation)));
    }

    // 계약 내용 무결성 검증 - 서명/결제 진행 전에 호출해, 저장된 contentHash와 현재 Reservation
    // 상태로 재계산한 해시가 다르면(=계약 체결 이후 내용이 변경됐으면) 처리를 막는다.
    public void verifyContentIntegrity(Contract contract) {
        String recomputed = buildContentHash(contract.getReservation());
        if (!recomputed.equals(contract.getContentHash())) {
            throw new ContractException(ContractErrorCode.CONTRACT_CONTENT_TAMPERED);
        }
    }

    // 계약 내용 해시(HMAC) 생성 - createPendingContract/verifyContentIntegrity가 동일 로직을 공유해야
    // 재계산 결과가 어긋나지 않는다.
    private String buildContentHash(Reservation reservation) {
        String payload = String.join(FIELD_SEPARATOR,
                String.valueOf(reservation.getId()), // 예약 ID
                String.valueOf(reservation.getSpace().getId()), // 공간 ID
                String.valueOf(reservation.getSpace().getHostId()), // 호스트 ID
                String.valueOf(reservation.getUser().getUserId()),  // 게스트 ID
                reservation.getStartDate().toString(), // LocalDate.toString()은 항상 yyyy-MM-dd 고정 포맷
                reservation.getEndDate().toString(),
                reservation.getUsagePurpose(),
                String.valueOf(reservation.getRentalFee()),
                String.valueOf(reservation.getDeposit()),
                String.valueOf(reservation.getInsuranceFee()),
                String.valueOf(reservation.getPlatformFee()),
                String.valueOf(reservation.getTotalPrice())
        );
        return cryptoService.hash(payload);
    }

    // 계약 예정 정보 조회
    public ContractResDTO.ContractInfoRes getContractInfo(User user, Long reservationId) {

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(()->new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 사용자의 예약인지 검사
        validateUserReservation(user, reservation);

        // 결제 정보 조회 (예약 정보 조회)
        UserMode currentMode = user.getCurrentMode();
        if (currentMode == UserMode.GUEST) {
            return ContractConverter.toGetGuestContractInfoRes(reservation);
        } else {
            return ContractConverter.toGetHostContractInfoRes(reservation);
        }

    }

    // 전자 서명 제출
    @Transactional
    public ContractResDTO.SignatureRes signature(User user, Long reservationId, ContractReqDTO.SignatureReq dto) {

        // 예약 조회 후 사용자의 예약인지 검사
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(()->new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        validateUserReservation(user, reservation);

        // 본인인증이 되었는지 검사: 본인인증 ciHash 조회
        String ciHash = identityVerificationService.getVerifiedCiHash(user)
                .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_SIGNER_NOT_VERIFIED));

        // 서명 이미지 해시 생성 - 업로드 API가 발급한 버킷·본인 소유 key prefix에 속하는 객체만 허용
        String expectedBucket = awsProperties.s3().bucket();
        String expectedKeyPrefix = UploadType.CONTRACT_SIGNATURE.getPath() + "/" + user.getUserId() + "/";
        String signatureImgHash = s3ObjectHasher.hash(dto.signatureUrl(), expectedBucket, expectedKeyPrefix);

        // 계약 조회 (예약이 승인되는 시점에 계약이 생성됨)
        Contract contract = contractRepository.findByReservation_Id(reservationId).orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

        // 서명 전, 계약 체결 이후 내용이 변조되지 않았는지 검증
        verifyContentIntegrity(contract);

        // 서명 이미지 저장
        // 호스트가 먼저 서명하고 나서 게스트 서명 가능 -> 계약 COMPLETE
        UserMode currentMode = user.getCurrentMode();
        try {
            if (currentMode == UserMode.HOST) {
                switch (contract.getStatus()) {
                    // 이미 모두 서명 처리되었는데 다시 요청할 경우
                    case PENDING_PAYMENT, COMPLETED -> throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_ALL_SIGNED);
                    // 이미 호스트 서명 처리되었는데 다시 요청할 경우
                    case GUEST_SIGNATURE_PENDING -> throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_HOST_SIGNED);
                    // 호스트 서명 처리
                    case HOST_SIGNATURE_PENDING -> contract.signByHost(ContractStatus.GUEST_SIGNATURE_PENDING, dto.signatureUrl(), ciHash, signatureImgHash);
                }
            }
            else if (currentMode == UserMode.GUEST) {
                switch (contract.getStatus()) {
                    // 이미 모두 서명 처리되었는데 다시 요청할 경우
                    case PENDING_PAYMENT, COMPLETED -> throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_ALL_SIGNED);
                    // 호스트가 먼저 서명해야하는데 그 전에 게스트가 먼저 요청한 경우
                    case HOST_SIGNATURE_PENDING -> throw new ContractException(ContractErrorCode.CONTRACT_NOT_GUEST_SIGNATURE_ORDER);
                    // 게스트 서명 처리
                    case GUEST_SIGNATURE_PENDING -> {
                        contract.signByGuest(ContractStatus.PENDING_PAYMENT, dto.signatureUrl(), ciHash, signatureImgHash);
                        reservation.markContractCompleted();
                    }
                }
            }

            // 동일 상태를 읽은 두 요청이 동시에 서명해도, 여기서 버전 충돌이 즉시 감지되도록 flush를 명시적으로 끌어옴
            contractRepository.saveAndFlush(contract);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ContractException(ContractErrorCode.CONTRACT_CONCURRENT_MODIFICATION);
        }

        return ContractResDTO.SignatureRes.builder()
                .contractStatus(contract.getStatus())
                .bothSigned(contract.getStatus() == ContractStatus.PENDING_PAYMENT)
                .build();
    }

    // 사용자(호스트/게스트)의 예약인지 검사
    private void validateUserReservation(User user, Reservation reservation) {

        // 예약 당시의 사용자 모드 확인
        boolean isGuest = reservation.getUser().getUserId().equals(user.getUserId());
        boolean isHost = reservation.getSpace().getHostId().equals(user.getUserId());

        // 예약의 사용자 모드가 실제 사용자 모드와 일치하는 지 검증
        boolean authorizedRole = (user.getCurrentMode() == UserMode.GUEST && isGuest) ||
                (user.getCurrentMode() == UserMode.HOST && isHost);

        if (!authorizedRole) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }
    }
}
