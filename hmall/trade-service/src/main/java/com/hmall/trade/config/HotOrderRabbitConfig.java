package com.hmall.trade.config;

import com.hmall.api.constants.HotOrderMQConstants;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HotOrderRabbitConfig {

    @Bean
    public DirectExchange hotOrderExchange() {
        return new DirectExchange(HotOrderMQConstants.HOT_ORDER_EXCHANGE_NAME, true, false);
    }
}
