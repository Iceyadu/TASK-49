package com.scholarops.security;

import com.scholarops.model.entity.Permission;
import com.scholarops.model.entity.Role;
import com.scholarops.model.entity.User;
import com.scholarops.model.entity.UserRole;
import com.scholarops.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<String> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        return new UserDetailsImpl(user, roles, permissions);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        Set<String> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        return new UserDetailsImpl(user, roles, permissions);
    }
}
