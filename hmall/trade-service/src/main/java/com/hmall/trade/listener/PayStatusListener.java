package com.hmall.trade.listener;

import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ClassName: PayStatusListener
 * Package: com.hmall.trade.listener
 * Description:
 *
 * @Author liang
 * @Create 2024/10/21 15:07
 * @Version jdk17.0
 */
@Component
@RequiredArgsConstructor
public class PayStatusListener {
    private final IOrderService orderService;

    /**
     * @Exchange(name = "pay.direct", type = ExchangeTypes.DIRECT)  // direct 类型（当前默认）
     * @Exchange(name = "pay.topic", type = ExchangeTypes.TOPIC)     // topic 类型
     * @Exchange(name = "pay.fanout", type = ExchangeTypes.FANOUT)   // fanout 类型
     * @param orderId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.pay.success.direct"),
            exchange = @Exchange(name = "pay.direct"),
            key = "pay.success"
    ))
    public void listenPayStatus(Long orderId){
        orderService.markOrderPaySuccess(orderId);
    }
}
