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

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.pay.success.direct"),
            exchange = @Exchange(name = "pay.direct"),
            key = "pay.success"
    ))
    public void listenPayStatus(Long orderId){
        orderService.markOrderPaySuccess(orderId);
    }
}
