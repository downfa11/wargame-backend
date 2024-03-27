package com.ns.wargame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling

public class WargameApplication {

	public static void main(String[] args) {
		SpringApplication.run(WargameApplication.class, args);
	}

}
