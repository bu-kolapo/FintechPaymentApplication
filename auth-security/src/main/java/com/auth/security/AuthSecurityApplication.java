package com.auth.security;

import com.auth.security.repository.UserAuthenticationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class AuthSecurityApplication {

    public static void main(String[] args) {

        SpringApplication.run(AuthSecurityApplication.class, args);
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        String encoded = encoder.encode("password123");
//        System.out.println(encoded);
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
