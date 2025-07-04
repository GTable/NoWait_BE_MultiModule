package com.nowait;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ApiAdminApplication {
	public static void main(String[] args) {
		org.springframework.boot.SpringApplication.run(ApiAdminApplication.class, args);
	}
}

