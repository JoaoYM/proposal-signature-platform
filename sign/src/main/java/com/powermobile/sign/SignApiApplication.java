package com.powermobile.sign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SignApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignApiApplication.class, args);
    }
}