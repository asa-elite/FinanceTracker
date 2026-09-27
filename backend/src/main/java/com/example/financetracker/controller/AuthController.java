package com.example.financetracker.controller;

import com.example.financetracker.dto.request.Register;
import com.example.financetracker.service.JwtService;
import com.example.financetracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Register reg) {
        try {
            userService.register(reg);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "User registered successfully.",
                            "email", reg.email()
                    )
            );
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "message", "An account with that email already exists."
                    )
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Register req) {
        try {
            Authentication auth = this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.email(),
                            req.password()
                    )
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            assert userDetails != null;
            String token = this.jwtService.generateToken(userDetails);

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "token", token,
                    "tokenType", "Bearer"
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(
                            "message", "Email or password is incorrect"
                    )
            );
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.LOCKED).body(
                    Map.of(
                            "message", String.format("User account %s is locked.", req.email())
                    )
            );
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(
                            "message", String.format("User account %s is disabled.", req.email())
                    )
            );
        }
    }

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("message", "api works");
    }
}
