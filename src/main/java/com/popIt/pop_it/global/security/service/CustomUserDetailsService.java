package com.popIt.pop_it.global.security.service;


import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService {

    // 추후 로그인 담당자의 구현 필요
    public UserDetails loadUserByUidAndSocialType(SocialProvider socialProvider, String uid) {
        return null;
    }

}
