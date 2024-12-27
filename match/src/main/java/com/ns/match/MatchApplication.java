package com.ns.match;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.ns.common","com.ns.match"})
public class MatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchApplication.class, args);
    }

}
