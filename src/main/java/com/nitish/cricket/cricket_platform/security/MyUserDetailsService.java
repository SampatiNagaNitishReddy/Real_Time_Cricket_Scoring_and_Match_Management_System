package com.nitish.cricket.cricket_platform.security;

import com.nitish.cricket.cricket_platform.model.User;
import com.nitish.cricket.cricket_platform.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getName())
                .password(user.getPassword())
                .authorities(Collections.emptyList()) // no roles for now
                .build();
    }
}