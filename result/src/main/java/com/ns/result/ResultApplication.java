package com.ns.result;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ns.common","com.ns.result"})
public class ResultApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResultApplication.class, args);
    }

}
