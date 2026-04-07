package com.hmall.api.client;

import com.hmall.api.po.Order;
import com.hmall.api.vo.OrderPayDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    /**
     * 方法名保持一致（虽然不是必须，但便于维护）
     * 路径完全匹配（考虑类级别的 @RequestMapping）
     * 参数注解和类型完全一致
     * 返回值类型兼容
     * @param order
     */
    @PostMapping("orders/update")
    void  updateOrderById(@RequestBody Order order);

    @GetMapping("orders/getPayOrderDetail/{id}")
    OrderPayDetailVO getPayOrderDetail(@PathVariable("id") Long orderId);
}
