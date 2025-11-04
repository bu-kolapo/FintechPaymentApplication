package com.auth.security.service;

import com.auth.security.model.UserAuthentication;
import com.auth.security.repository.UserAuthenticationRepository;
import com.auth.security.util.JwtUtil;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Service
@Primary
public class LoginServiceImplementation implements ReactiveUserDetailsService {

    private  final UserAuthenticationRepository userAuthenticationRepository;
    private  final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginServiceImplementation(UserAuthenticationRepository userAuthenticationRepository,JwtUtil jwtUtil,PasswordEncoder passwordEncoder) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.jwtUtil=jwtUtil;
        this.passwordEncoder=passwordEncoder;
        System.out.println("✅ LoginServiceImplementation bean created successfully!");
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        System.out.println("Inside findByUsername for: " + username);
        return userAuthenticationRepository.findByUsername(username)
                .doOnNext(u -> System.out.println("Found user: " + u.getUsername()))
                .switchIfEmpty(Mono.error(new BadCredentialsException("User not found")))
                .map(user -> new User(
                        user.getUsername(),
                        user.getPassword(),
                        true, true, true, true,
                        new ArrayList<>()
                ));
    }

    // ✅ This method performs login verification and returns JWT token
    public Mono<String> loginAndGenerateToken(String username, String rawPassword) {
        return userAuthenticationRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new BadCredentialsException("User not found")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                        return Mono.error(new BadCredentialsException("Invalid password"));
                    }
                    return Mono.just(jwtUtil.generateToken(user.getUsername()));
                });
    }


}

