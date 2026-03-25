package com.hmall.api.client;

import com.hmall.api.po.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * ClassName: TradeClient
 * Package: com.hmall.api.client
 * Description:
 *
 * @Author liang
 * @Create 2024/10/13 15:33
 * @Version jdk17.0
 */
@FeignClient("trade-service")
public interface TradeClient {
    @PostMapping("orders/update")
    void  updateOrderById(@RequestBody Order order);
}
