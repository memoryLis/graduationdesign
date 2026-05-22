package com.hmall.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.api.dto.OrderFormDTO;
import com.hmall.api.dto.UserOrderInteractionDTO;
import com.hmall.api.vo.OrderPayDetailVO;
import com.hmall.trade.domain.po.Order;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO);

    void markOrderPaySuccess(Long orderId);

    void cancelOrder(Long orderId);

    OrderPayDetailVO queryPayOrderDetail(Long orderId);

    List<UserOrderInteractionDTO> queryMyOrderInteractions();

    List<UserOrderInteractionDTO> queryOrderInteractions();

    void confirmOrder(Long orderId);
}
