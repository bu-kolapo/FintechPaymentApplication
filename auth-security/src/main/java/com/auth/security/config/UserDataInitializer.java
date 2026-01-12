package com.auth.security.config;



import com.auth.security.model.UserAuthentication;
import com.auth.security.repository.UserAuthenticationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {

    private final UserAuthenticationRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        UserAuthentication user = UserAuthentication.builder()
                .username("tobi_dally")
                .password(passwordEncoder.encode("p@$sw0rd"))
                .role("ROLE_USER")
                .build();

        userRepo.save(user)
                .doOnNext(u -> System.out.println("User saved: " + u))
                .subscribe();
    }
}

