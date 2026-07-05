package com.popIt.pop_it.global.security.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuthService extends DefaultOAuth2UserService {

    // 추후 로그인 담당자의 구현 필요
}
