package com.hmall.trade.controller;

import com.hmall.api.client.UserClient;
import com.hmall.api.dto.AddressDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.exception.ForbiddenException;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.domain.dto.LogisticsDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.domain.po.OrderLogistics;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderLogisticsService;
import com.hmall.trade.service.IOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Api(tags = "物流管理接口")
@RestController
@RequestMapping("/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final IOrderLogisticsService logisticsService;
    private final IOrderService orderService;
    private final IOrderDetailService orderDetailService;
    private final UserClient userClient;

    @ApiOperation("管理员发货")
    @PostMapping("/ship/{orderId}")
    public void shipOrder(@PathVariable("orderId") Long orderId,
                          @RequestBody LogisticsDTO logisticsDTO) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BadRequestException("订单不存在");
        }
        if (order.getStatus() != 2) {
            throw new BadRequestException("订单状态不正确，仅已付款的订单可发货");
        }

        OrderDetail orderDetail = orderDetailService.lambdaQuery()
                .eq(OrderDetail::getOrderId, orderId)
                .last("limit 1")
                .one();
        if (orderDetail == null) {
            throw new BadRequestException("订单详情不存在");
        }

        AddressDTO address = userClient.queryAddressById(orderDetail.getAddressId());
        if (address == null) {
            throw new BadRequestException("收货地址不存在");
        }

        OrderLogistics logistics = new OrderLogistics();
        logistics.setOrderId(orderId);
        logistics.setLogisticsCompany(logisticsDTO.getLogisticsCompany());
        logistics.setLogisticsNumber(logisticsDTO.getLogisticsNumber());
        logistics.setContact(address.getContact() != null ? address.getContact() : "");
        logistics.setMobile(address.getMobile() != null ? address.getMobile() : "");
        logistics.setProvince(address.getProvince() != null ? address.getProvince() : "");
        logistics.setCity(address.getCity() != null ? address.getCity() : "");
        logistics.setTown(address.getTown() != null ? address.getTown() : "");
        logistics.setStreet(address.getStreet() != null ? address.getStreet() : "");
        logistics.setCreateTime(LocalDateTime.now());
        logistics.setUpdateTime(LocalDateTime.now());
        logisticsService.saveOrUpdate(logistics);

        order.setStatus(3);
        order.setConsignTime(LocalDateTime.now());
        orderService.updateById(order);
    }

    @ApiOperation("查询订单物流信息")
    @GetMapping("/{orderId}")
    public OrderLogistics queryLogistics(@PathVariable("orderId") Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BadRequestException("订单不存在");
        }

        Long userId = UserContext.getUser();
        if (!order.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new ForbiddenException("无权查看此订单物流信息");
        }

        return logisticsService.getById(orderId);
    }
}
