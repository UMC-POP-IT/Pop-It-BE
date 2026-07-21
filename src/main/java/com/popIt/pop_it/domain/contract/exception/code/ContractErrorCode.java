package com.popIt.pop_it.domain.contract.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContractErrorCode implements BaseErrorCode {

    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTRACT404_1", "계약을 찾을 수 없습니다."),
    CONTRACT_ALREADY_HOST_SIGNED(HttpStatus.CONFLICT, "CONTRACT409_1", "이미 호스트 서명 처리 되었습니다."),
    CONTRACT_ALREADY_ALL_SIGNED(HttpStatus.CONFLICT, "CONTRACT409_2", "이미 모두(호스트, 게스트) 서명 처리 되었습니다."),
    CONTRACT_NOT_GUEST_SIGNATURE_ORDER(HttpStatus.CONFLICT, "CONTRACT409_3", "아직 게스트 서명 순서가 되지 않았습니다. 호스트가 먼저 서명해야 게스트가 서명할 수 있습니다."),
    CONTRACT_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "CONTRACT409_4", "다른 요청에 의해 이미 처리된 서명입니다. 새로고침 후 다시 시도해주세요."),
    CONTRACT_SIGNER_NOT_VERIFIED(HttpStatus.CONFLICT, "CONTRACT409_5", "본인인증이 완료되지 않아 서명을 할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
