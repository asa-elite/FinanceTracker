package com.example.financetracker.service;

import com.example.financetracker.dto.request.Register;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder encoder;

    public UserService(UserDetailsManager userDetailsManager, PasswordEncoder encoder) {
        this.userDetailsManager = userDetailsManager;
        this.encoder = encoder;
    }

    public void register(Register reg) throws ResponseStatusException {
        if (this.userDetailsManager.userExists(reg.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );
        }
        UserDetails user = User.builder()
                .username(reg.email())
                .password(this.encoder.encode(reg.password()))
                .roles("USER")
                .build();
        this.userDetailsManager.createUser(user);
    }
}
