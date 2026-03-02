package org.example.rlplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RlPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RlPlatformApplication.class, args);
    }

}
