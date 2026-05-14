package com.humangc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.humangc.mapper")
public class HumanGCApplication {

    public static void main(String[] args) {
        SpringApplication.run(HumanGCApplication.class, args);
    }
}
