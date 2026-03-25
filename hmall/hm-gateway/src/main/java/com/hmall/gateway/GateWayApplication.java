package com.hmall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.AntPathMatcher;

/**
 * ClassName: GateWayAPPlication
 * Package: com.hmall.gateway
 * Description:
 *
 * @Author liang
 * @Create 2024/10/13 16:46
 * @Version jdk17.0
 */
@SpringBootApplication
public class GateWayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GateWayApplication.class,args);
    }

    @Bean
    public AntPathMatcher antPathMatcher() {

        return new AntPathMatcher();
    }
}
