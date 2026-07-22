package com.powermobile.crm;

import org.springframework.boot.SpringApplication;

public class TestCrmApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(CrmApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
