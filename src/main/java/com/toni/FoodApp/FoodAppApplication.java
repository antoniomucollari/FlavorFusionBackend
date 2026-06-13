package com.toni.FoodApp;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
@RequiredArgsConstructor
public class FoodAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(FoodAppApplication.class, args);
	}
}
