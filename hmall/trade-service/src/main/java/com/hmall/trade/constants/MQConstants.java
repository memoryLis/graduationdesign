package com.hmall.trade.constants;

/**
 * ClassName: MQConstants
 * Package: com.hmall.trade.constants
 * Description:
 *
 * @Author liang
 * @Create 2024/10/24 14:26
 * @Version jdk17.0
 */
public interface MQConstants {
    String DELAY_EXCHANGE_NAME="trade.delay.direct";
    String DELAY_QUEUE_NAME="trade.delay.order.queue";
    String DELAY_ROUTING_KEY="delay.order.query";
}
