package com.ns.resultquery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ns.common","com.ns.resultquery"})
public class ResultQueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResultQueryApplication.class, args);
    }

}
