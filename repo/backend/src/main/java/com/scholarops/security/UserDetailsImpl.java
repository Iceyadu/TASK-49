package com.scholarops.security;

import com.scholarops.model.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean accountLocked;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Set<String> permissions;

    public UserDetailsImpl(User user, Set<String> roles, Set<String> permissions) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.enabled = user.getEnabled();
        this.accountLocked = user.getAccountLocked();
        this.permissions = permissions;

        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
