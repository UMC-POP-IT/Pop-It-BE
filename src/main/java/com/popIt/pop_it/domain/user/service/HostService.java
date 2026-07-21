package com.popIt.pop_it.domain.user.service;

import com.popIt.pop_it.domain.user.dto.HostRegisterRequest;
import com.popIt.pop_it.domain.user.dto.HostRegisterResponse;

public interface HostService {

    // 인증된 사용자를 호스트로 등록하고 활동 모드를 HOST로 전환한다
    HostRegisterResponse register(Long userId, HostRegisterRequest request);
}
