package com.auth.security;

import com.auth.security.repository.UserAuthenticationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.auth.security",
        "com.commonlib"          // <-- ADD THIS
})
@EnableReactiveMongoRepositories
public class AuthSecurityApplication {

    public static void main(String[] args) {

        SpringApplication.run(AuthSecurityApplication.class, args);

    }

    @Bean
    CommandLineRunner testLogin(UserAuthenticationRepository userRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            String testUsername = "john.doe";
            String rawPassword = "password123";

            userRepo.findByUsername(testUsername)
                    .doOnNext(user -> {
//                        String rawPassword = "password123";
                        String hash = passwordEncoder.encode(rawPassword);
                        System.out.println("NEW HASH TO STORE: " + hash);
                        System.out.println("USER FOUND: " + user.getUsername());
                        System.out.println("STORED HASH: " + user.getPassword());
                        System.out.println("MATCH RESULT: " +
                                passwordEncoder.matches(rawPassword, user.getPassword()));
                    })
                    .doOnError(e -> System.out.println("ERROR: " + e.getMessage()))
                    .subscribe(); // Important! Mono will not run without subscription
        };
    }



    @Component
    public class StartupRunner implements CommandLineRunner {
        @Autowired
        UserAuthenticationRepository repo;
        public void run(String... args) {
            repo.findAll().doOnNext(u -> System.out.println("User: " + u)).subscribe();
        }
    }


}
