package com.auth.security.controller;

import com.auth.security.model.UserAuthentication;
import com.auth.security.service.LoginServiceImplementation;
import com.commonlib.util.JwtUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final LoginServiceImplementation userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;



        @PostMapping("/login")
        public Mono<ResponseEntity<Map<String, String>>> login(@RequestBody UserAuthentication userAuthentication) {




            String username = userAuthentication.getUsername();
            String password = userAuthentication.getPassword();

            System.out.println("🔥 Inside AuthController.login()");
            System.out.println("Username: " + username);

            return userDetailsService.loginAndGenerateToken(username, password)
                    .map(token -> ResponseEntity.ok(Map.of("token", token)))
                    .onErrorResume(e -> Mono.just(
                            ResponseEntity.status(401).body(Map.of("error", e.getMessage()))
                    ));
        }
}
