package com.popIt.pop_it.domain.user_agreement.service;

import com.popIt.pop_it.domain.user_agreement.dto.AgreementReqDTO;
import com.popIt.pop_it.domain.user_agreement.dto.AgreementResDTO;

public interface AgreementService {

    // 로그인한 사용자의 약관 동의 정보를 저장한다 (기존 이력이 있으면 갱신)
    AgreementResDTO.SaveResult save(Long userId, AgreementReqDTO.Save request);
}
