package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.trade.constants.MQConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Component;

/**
 * ClassName: OrderDelayListener
 * Package: com.hmall.trade.listener
 * Description:
 *
 * @Author liang
 * @Create 2024/10/24 14:45
 * @Version jdk17.0
 */
// @Component
@RequiredArgsConstructor
public class OrderDelayListener {
    private final IOrderService orderService;
    private final PayClient payClient;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE_NAME,delayed = "true"),//rabbitmq需要安装延迟插件
            key = MQConstants.DELAY_ROUTING_KEY
    ))
    public void listenOrderDelay(Long orderId) {
        //查询订单
        Order order = orderService.getById(orderId);
        if(order == null || order.getStatus()!=1) {
            return;
        }
        // 查询支付流水
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        if(payOrderDTO != null && payOrderDTO.getStatus() ==3){
            //支付成功,标记订单为已支付
            orderService.markOrderPaySuccess(orderId);
        }else {
            //支付失败
            orderService.cancelOrder(orderId);

        }

    }
}
