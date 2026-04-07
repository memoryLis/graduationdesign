package com.hmall.trade.controller;

import com.hmall.api.vo.OrderPayDetailVO;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName: OrderFeignController
 * Package: com.hmall.trade.controller
 *
 * @Author liang
 * @Create 2026/4/6 17:39
 * Description: 通过openfeign调用该服务
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderFeignController {
    private final IOrderService orderService;

    @GetMapping("/getPayOrderDetail/{id}")
    public OrderPayDetailVO getPayOrderDetail(@PathVariable("id") Long orderId){
        return orderService.queryPayOrderDetail(orderId);
    }
}
