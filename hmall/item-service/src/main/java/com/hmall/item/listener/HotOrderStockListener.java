package com.hmall.item.listener;

import com.hmall.api.constants.HotOrderMQConstants;
import com.hmall.api.mq.HotItemStockMessage;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.controller.HotItemController;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotOrderStockListener {

    private final IItemService itemService;
    private final RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = HotOrderMQConstants.HOT_ORDER_STOCK_QUEUE_NAME),
            exchange = @Exchange(name = HotOrderMQConstants.HOT_ORDER_EXCHANGE_NAME),
            key = HotOrderMQConstants.HOT_ORDER_STOCK_ROUTING_KEY
    ))
    public void listenHotOrderStock(HotItemStockMessage message) {
        if (message == null || message.getDetails() == null || message.getDetails().isEmpty()) {
            return;
        }
        itemService.deductStock(message.getDetails());
        message.getDetails().forEach(detail -> syncHotItemCache(detail.getItemId()));
    }

    private void syncHotItemCache(Long itemId) {
        Object hotValue = redisTemplate.opsForHash().get(HotItemController.HOT_ITEM_KEY, String.valueOf(itemId));
        if (hotValue == null) {
            redisTemplate.delete(HotItemController.HOT_ITEM_STOCK_KEY_PREFIX + itemId);
            return;
        }
        Item latestItem = itemService.getById(itemId);
        if (latestItem == null) {
            redisTemplate.opsForHash().delete(HotItemController.HOT_ITEM_KEY, String.valueOf(itemId));
            redisTemplate.delete(HotItemController.HOT_ITEM_STOCK_KEY_PREFIX + itemId);
            return;
        }
        ItemDTO dto = BeanUtils.copyBean(latestItem, ItemDTO.class);
        dto.setHot(Boolean.TRUE);
        redisTemplate.opsForHash().put(HotItemController.HOT_ITEM_KEY, String.valueOf(itemId), dto);
        redisTemplate.opsForValue().set(HotItemController.HOT_ITEM_STOCK_KEY_PREFIX + itemId, String.valueOf(latestItem.getStock()));
    }
}
