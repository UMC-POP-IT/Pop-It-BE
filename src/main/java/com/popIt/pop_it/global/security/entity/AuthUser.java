package com.popIt.pop_it.global.security.entity;

import com.popIt.pop_it.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class AuthUser implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getCurrentMode().name()));
    }

    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return user.getSocialUid();
    }
}
