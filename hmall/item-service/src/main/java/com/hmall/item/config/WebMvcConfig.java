package com.hmall.item.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir") + "/static/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);

        /**
         * 1. 管理员上传图片 abc.jpg
         *    ↓
         * 2. 服务器保存到: F:\...\item-service\static\\uuid-abc.jpg
         *    ↓
         * 3. 返回给前端: "/uploads/uuid-abc.jpg"
         *    ↓
         * 4. 前端显示: <img src="http://localhost:8081/uploads/uuid-abc.jpg">
         *    ↓
         * 5. Spring 拦截 /uploads/** 请求
         *    ↓
         * 6. 从磁盘读取 static/uuid-abc.jpg 返回给浏览器
         */
    }
}
