package com.hmall.api.config;

import com.hmall.api.client.fackback.ItemClientFallbackFactory;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * ClassName: DeafultFeginConfig
 * Package: com.hmall.api.config
 * Description:
 *
 * @Author liang
 * @Create 2024/10/12 17:08
 * @Version jdk17.0
 */
public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 对OpenFeign 进行配置，使其对其他服务发送的http请求带请求头
     *
     * @return
     */
    // @Bean
    public RequestInterceptor userinfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                // Long userId = UserContext.getUser();
                Long userId = 66666l;
                if (userId != null) {
                    requestTemplate.header("user-info", userId.toString());
                }
            }
        };

    }
    @Bean
    public ItemClientFallbackFactory itemClientFallbackFactory(){
           return  new ItemClientFallbackFactory();
    }

}
