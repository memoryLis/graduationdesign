package com.hmall.cart.listener;

import com.hmall.api.constants.HotOrderMQConstants;
import com.hmall.api.mq.CartClearMessage;
import com.hmall.cart.service.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotOrderCartClearListener {

    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = HotOrderMQConstants.HOT_ORDER_CART_QUEUE_NAME),
            exchange = @Exchange(name = HotOrderMQConstants.HOT_ORDER_EXCHANGE_NAME),
            key = HotOrderMQConstants.HOT_ORDER_CART_ROUTING_KEY
    ))
    public void listenCartClear(CartClearMessage message) {
        if (message == null || message.getUserId() == null || message.getItemIds() == null || message.getItemIds().isEmpty()) {
            return;
        }
        cartService.removeByItemIdsForUser(message.getUserId(), message.getItemIds());
    }
}
