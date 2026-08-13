package com.popIt.pop_it.domain.user.service;

import com.popIt.pop_it.domain.user.dto.UserResDTO;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;

public interface UserService {

    // 로그인한 사용자의 활동 모드를 전환한다 (HOST 전환 시 호스트 프로필 필요, 멱등)
    UserResDTO.UserInfoRes switchMode(Long userId, UserMode mode);

    // 로그인한 사용자의 내 정보를 조회한다 (세션 복구/토큰 유효성 프로브 용도)
    UserResDTO.UserInfoRes getMyInfo(Long userId);
}
