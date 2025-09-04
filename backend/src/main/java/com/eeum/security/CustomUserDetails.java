package com.eeum.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.eeum.dto.User;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Integer userId;
    private final String loginId;
    private final String password;
    private final String nickname;

    public CustomUserDetails(Integer userId, String loginId, String password, String nickname) {
        this.userId = userId;
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
    }

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(
                user.getUserId(),
                user.getLoginId(),
                user.getPassword(),
                user.getNickname()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getUsername() { return loginId; }
    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
