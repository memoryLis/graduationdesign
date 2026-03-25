package com.hmall.user.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName: TestController
 * Package: com.hmall.user.controller
 * Description:
 *
 * @Author liang
 * @Create 2025/10/30 14:38
 * @Version jdk17.0
 */
@RequestMapping("api")
@RestController
@Api(tags = "testfeign")
public class TestController {
     @Autowired
     Feign feign;
     @GetMapping("test")
     @ApiOperation("测试1")
      public String test(){
      return "hello world i am user-service";
     }
     @ApiOperation("testfeign")
     @GetMapping("test2")
      public String test2(){
         return feign.get();
     }
}
