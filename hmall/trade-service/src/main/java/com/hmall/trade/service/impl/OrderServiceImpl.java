package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.ItemClient;
import com.hmall.api.client.UserClient;
import com.hmall.api.constants.HotOrderMQConstants;
import com.hmall.api.dto.AddressDTO;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.dto.OrderFormDTO;
import com.hmall.api.dto.UserOrderInteractionDTO;
import com.hmall.api.mq.CartClearMessage;
import com.hmall.api.mq.HotItemStockMessage;
import com.hmall.api.vo.OrderItemDetailVO;
import com.hmall.api.vo.OrderPayDetailVO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderDetailMapper;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private static final String HOT_ITEM_KEY = "hot_items";
    private static final String HOT_ITEM_STOCK_KEY_PREFIX = "hot_item_stock:";

    private static final DefaultRedisScript<Long> HOT_STOCK_DEDUCT_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]);" +
                    "if (not current) then return -1; end;" +
                    "current = tonumber(current);" +
                    "if (not current) then return -1; end;" +
                    "local num = tonumber(ARGV[1]);" +
                    "if (current < num) then return 0; end;" +
                    "return redis.call('DECRBY', KEYS[1], num);",
            Long.class
    );

    private final ItemClient itemClient;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final IOrderDetailService detailService;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        if (CollUtils.isEmpty(detailDTOS)) {
            throw new BadRequestException("下单商品不能为空");
        }

        Long userId = UserContext.getUser();
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum, Integer::sum, LinkedHashMap::new));
        Set<Long> itemIds = itemNumMap.keySet();

        List<ItemDTO> hotItems = loadHotItems(itemIds);
        Map<Long, ItemDTO> hotItemMap = hotItems.stream()
                .collect(Collectors.toMap(ItemDTO::getId, item -> item, (a, b) -> a));

        List<OrderDetailDTO> hotDetails = detailDTOS.stream()
                .filter(detail -> hotItemMap.containsKey(detail.getItemId()))
                .collect(Collectors.toList());
        List<OrderDetailDTO> normalDetails = detailDTOS.stream()
                .filter(detail -> !hotItemMap.containsKey(detail.getItemId()))
                .collect(Collectors.toList());

        deductHotStockInRedis(hotDetails);

        List<ItemDTO> normalItems = CollUtils.emptyList();
        if (CollUtils.isNotEmpty(normalDetails)) {
            List<Long> normalItemIds = normalDetails.stream().map(OrderDetailDTO::getItemId).collect(Collectors.toList());
            normalItems = itemClient.queryItemsByIds(normalItemIds);
            if (normalItems == null || normalItems.size() < normalItemIds.size()) {
                throw new BadRequestException("商品不存在");
            }
        }

        List<ItemDTO> allItems = new ArrayList<>(hotItems.size() + normalItems.size());
        allItems.addAll(hotItems);
        allItems.addAll(normalItems);
        if (allItems.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }

        int total = 0;
        for (ItemDTO item : allItems) {
            Integer num = itemNumMap.get(item.getId());
            if (num != null) {
                total += item.getPrice() * num;
            }
        }

        Order order = new Order();
        order.setTotalFee(total);
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(userId);
        order.setStatus(1);
        save(order);

        List<OrderDetail> details = buildDetails(order.getId(), orderFormDTO.getAddressId(), allItems, itemNumMap);
        detailService.saveBatch(details);

        if (CollUtils.isNotEmpty(normalDetails)) {
            try {
                itemClient.deductStock(normalDetails);
            } catch (Exception e) {
                throw new RuntimeException("库存不足", e);
            }
        }

        sendCartClearMessage(userId, itemIds);
        if (CollUtils.isNotEmpty(hotDetails)) {
            sendHotStockSyncMessage(order.getId(), hotDetails);
        }

        return order.getId();
    }

    private List<ItemDTO> loadHotItems(Collection<Long> itemIds) {
        if (CollUtils.isEmpty(itemIds)) {
            return CollUtils.emptyList();
        }
        List<Object> hashKeys = itemIds.stream().map(String::valueOf).collect(Collectors.toList());
        List<Object> hotItemValues = redisTemplate.opsForHash().multiGet(HOT_ITEM_KEY, hashKeys);
        if (CollUtils.isEmpty(hotItemValues)) {
            return CollUtils.emptyList();
        }

        return hotItemValues.stream()
                .filter(Objects::nonNull)
                .map(value -> BeanUtils.copyBean(value, ItemDTO.class))
                .filter(item -> item.getId() != null)
                .collect(Collectors.toList());
    }

    private void deductHotStockInRedis(List<OrderDetailDTO> hotDetails) {
        for (OrderDetailDTO detail : hotDetails) {
            String stockKey = HOT_ITEM_STOCK_KEY_PREFIX + detail.getItemId();
            Long result = stringRedisTemplate.execute(
                    HOT_STOCK_DEDUCT_SCRIPT,
                    Collections.singletonList(stockKey),
                    String.valueOf(detail.getNum())
            );
            if (result == null || result < 0) {
                throw new BadRequestException("热门商品库存缓存不存在，请联系管理员重新同步热门商品");
            }
            if (result == 0) {
                throw new BadRequestException("热门商品库存不足");
            }
        }
    }

    private void sendHotStockSyncMessage(Long orderId, List<OrderDetailDTO> hotDetails) {
        HotItemStockMessage message = new HotItemStockMessage();
        message.setOrderId(orderId);
        message.setDetails(hotDetails);
        rabbitTemplate.convertAndSend(
                HotOrderMQConstants.HOT_ORDER_EXCHANGE_NAME,
                HotOrderMQConstants.HOT_ORDER_STOCK_ROUTING_KEY,
                message
        );
    }

    private void sendCartClearMessage(Long userId, Collection<Long> itemIds) {
        CartClearMessage message = new CartClearMessage();
        message.setUserId(userId);
        message.setItemIds(new ArrayList<>(itemIds));
        rabbitTemplate.convertAndSend(
                HotOrderMQConstants.HOT_ORDER_EXCHANGE_NAME,
                HotOrderMQConstants.HOT_ORDER_CART_ROUTING_KEY,
                message
        );
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public void cancelOrder(Long orderId) {
        LambdaUpdateWrapper<Order> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Order::getId, orderId).set(Order::getStatus, 5);
        orderMapper.update(null, lambdaUpdateWrapper);

        LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, orderId);
        OrderDetail orderDetail = orderDetailMapper.selectOne(orderDetailLambdaQueryWrapper);
        System.out.println(orderDetail.getName() + orderDetail.getNum());
    }

    private List<OrderDetail> buildDetails(Long orderId, Long addressId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setAddressId(addressId);
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }

    @Override
    public OrderPayDetailVO queryPayOrderDetail(Long orderId) {
        Long userId = UserContext.getUser();
        boolean isAdmin = UserContext.isAdmin();
        Order order;
        if (isAdmin) {
            order = lambdaQuery().eq(Order::getId, orderId).one();
        } else {
            order = lambdaQuery()
                    .eq(Order::getId, orderId)
                    .eq(Order::getUserId, userId)
                    .one();
        }

        if (order == null) {
            throw new BadRequestException("订单不存在");
        }

        List<OrderDetail> details = detailService.lambdaQuery()
                .eq(OrderDetail::getOrderId, orderId)
                .list();

        OrderPayDetailVO result = new OrderPayDetailVO();
        result.setOrderId(order.getId());
        result.setUserId(order.getUserId());
        result.setTotalFee(order.getTotalFee());
        result.setPaymentType(order.getPaymentType());
        result.setOrderStatus(order.getStatus());
        result.setPayTime(order.getPayTime());
        result.setCreateTime(order.getCreateTime());
        Long addressId = details.isEmpty() ? null : details.get(0).getAddressId();
        if (addressId != null) {
            AddressDTO address = userClient.queryAddressById(addressId);
            result.setAddress(address);
        }

        List<OrderItemDetailVO> detailVOList = details.stream().map(d -> {
            OrderItemDetailVO detailVO = BeanUtils.copyBean(d, OrderItemDetailVO.class);
            detailVO.setItemId(d.getItemId());
            return detailVO;
        }).collect(Collectors.toList());
        result.setDetails(detailVOList);
        return result;
    }

    @Override
    public List<UserOrderInteractionDTO> queryMyOrderInteractions() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return CollUtils.emptyList();
        }
        List<Order> orders = lambdaQuery()
                .eq(Order::getUserId, userId)
                .in(Order::getStatus, 2, 3, 4, 6)
                .list();
        if (CollUtils.isEmpty(orders)) {
            return CollUtils.emptyList();
        }

        Map<Long, Long> orderUserMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, Order::getUserId, (a, b) -> a));

        List<OrderDetail> details = detailService.lambdaQuery()
                .in(OrderDetail::getOrderId, orderUserMap.keySet())
                .list();
        if (CollUtils.isEmpty(details)) {
            return CollUtils.emptyList();
        }

        return details.stream()
                .map(detail -> {
                    UserOrderInteractionDTO dto = new UserOrderInteractionDTO();
                    dto.setUserId(orderUserMap.get(detail.getOrderId()));
                    dto.setItemId(detail.getItemId());
                    dto.setNum(detail.getNum());
                    return dto;
                })
                .filter(dto -> dto.getUserId() != null && dto.getItemId() != null)
                .collect(Collectors.toList());
    }

    @Override
    public void confirmOrder(Long orderId) {
        Long userId = UserContext.getUser();
        Order order = lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId)
                .one();
        if (order == null) {
            throw new BadRequestException("订单不存在");
        }
        if (order.getStatus() != 3) {
            throw new BadRequestException("订单状态不正确，仅已发货的订单可确认收货");
        }
        order.setStatus(4);
        order.setEndTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public List<UserOrderInteractionDTO> queryOrderInteractions() {
        List<Order> orders = lambdaQuery()
                .in(Order::getStatus, 2, 3, 4, 6)
                .list();
        if (CollUtils.isEmpty(orders)) {
            return CollUtils.emptyList();
        }

        Map<Long, Long> orderUserMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, Order::getUserId, (a, b) -> a));

        List<OrderDetail> details = detailService.lambdaQuery()
                .in(OrderDetail::getOrderId, orderUserMap.keySet())
                .list();
        if (CollUtils.isEmpty(details)) {
            return CollUtils.emptyList();
        }

        return details.stream()
                .map(detail -> {
                    UserOrderInteractionDTO dto = new UserOrderInteractionDTO();
                    dto.setUserId(orderUserMap.get(detail.getOrderId()));
                    dto.setItemId(detail.getItemId());
                    dto.setNum(detail.getNum());
                    return dto;
                })
                .filter(dto -> dto.getUserId() != null && dto.getItemId() != null)
                .collect(Collectors.toList());
    }
}
