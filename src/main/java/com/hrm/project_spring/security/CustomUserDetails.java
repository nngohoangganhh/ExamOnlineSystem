package com.hrm.project_spring.security;

import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public UserStatus getStatus() {
        return user.getStatus();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));

            if (role.getPermissions() != null) {
                role.getPermissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                        .forEach(authorities::add);
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}