package com.example.serviceonec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ServiceonecApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceonecApplication.class, args);
	}

}
