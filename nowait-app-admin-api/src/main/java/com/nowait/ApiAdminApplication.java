package com.nowait;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@EnableFeignClients
@SpringBootApplication
public class ApiAdminApplication {
	public static void main(String[] args) {
		org.springframework.boot.SpringApplication.run(ApiAdminApplication.class, args);
	}
}


