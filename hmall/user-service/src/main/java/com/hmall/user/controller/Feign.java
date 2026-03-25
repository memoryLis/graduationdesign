package com.hmall.user.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ClassName: Feign
 * Package: com.hmall.user.controller
 * Description:
 *
 * @Author liang
 * @Create 2025/10/30 14:39
 * @Version jdk17.0
 */
@FeignClient("liang-service")
public interface Feign {
     @GetMapping("api/test")
     String get();

}
