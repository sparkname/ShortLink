package com.lin.shortlinkservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lin.shortlinkservice.mapper")
public class ShortLinkServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortLinkServiceApplication.class, args);
    }

}
