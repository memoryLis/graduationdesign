package com.hmall.api.constants;

public interface HotOrderMQConstants {
    String HOT_ORDER_EXCHANGE_NAME = "trade.hot.order.direct";

    String HOT_ORDER_STOCK_QUEUE_NAME = "trade.hot.order.stock.queue";
    String HOT_ORDER_STOCK_ROUTING_KEY = "hot.order.stock";

    String HOT_ORDER_CART_QUEUE_NAME = "trade.hot.order.cart.queue";
    String HOT_ORDER_CART_ROUTING_KEY = "hot.order.cart";
}
