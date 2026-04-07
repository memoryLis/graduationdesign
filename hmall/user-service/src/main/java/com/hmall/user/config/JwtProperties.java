package com.hmall.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "hm.jwt")
/**
 * 该注解需要被激活
 * 1 通过 @EnableConfigurationProperties 激活
 * 2 通过 @Component 激活
 */
public class  JwtProperties {
    private Resource location;
    private String password;
    private String alias;
    private Duration tokenTTL = Duration.ofMinutes(600);
}
