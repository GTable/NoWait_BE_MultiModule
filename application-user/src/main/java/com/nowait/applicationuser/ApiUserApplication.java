package com.nowait.applicationuser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
	"com.nowait.applicationuser",
	"com.nowait.frontsecurity",
	"com.nowait.externaloauth"
})
@EntityScan(basePackages = {
	"com.nowait.menu.entity",
	"com.nowait.store.entity",
	"com.nowait.token.entity",
	"com.nowait.user.entity",
	"com.nowait.bookmark.entity",
	"com.nowait.reservation.entity",
	"com.nowait.order.entity",
	"com.nowait.order.entity"
})
@EnableJpaRepositories(basePackages = {
	"com.nowait.menu.repository",
	"com.nowait.user.repository",
	"com.nowait.store.repository",
	"com.nowait.token.repository",
	"com.nowait.bookmark.repository",
	"com.nowait.order.repository",
	"com.nowait.reservation.repository"
})
public class ApiUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiUserApplication.class, args);
	}

}
