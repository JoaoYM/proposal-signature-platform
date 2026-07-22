package com.powermobile.sign;

import org.springframework.boot.SpringApplication;

public class TestSignApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SignApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
