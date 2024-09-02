package com.ns.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ns.common","com.ns.membership"})
public class MembershipApplication {

    public static void main(String[] args) {
        SpringApplication.run(MembershipApplication.class, args);
    }

}
