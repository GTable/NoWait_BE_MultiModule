package com.example.apiadmin;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
	"com.example.apiadmin",
	"com.example.infrastorage",
	"com.nowait.adminsecurity"
})
@EntityScan(basePackages = {
	"com.example.menu.entity",         // domain-menu
	"com.example.domainstore.entity",  // domain-store
	"com.example.domaintoken.entity",
	"com.nowaiting.user.entity",
	"com.nowait.domainreservation.entity",
	"com.nowait.domainorder.entity",
	"com.nowait.domainorder.entity"

})
@EnableJpaRepositories(basePackages = {
	"com.example.menu.repository",
	"com.nowaiting.user.repository",
	"com.example.domainstore.repository",
	"com.example.domaintoken.repository",
	"com.nowait.domainorder.repository",
	"com.nowait.domainreservation.repository"
})
public class ApiAdminApplication {
	public static void main(String[] args) {
		org.springframework.boot.SpringApplication.run(ApiAdminApplication.class, args);
	}
}
