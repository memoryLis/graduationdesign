package com.hmall.api.vo;

import com.hmall.api.dto.AddressDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@ApiModel(description = "支付单对应订单详情")
@Data
public class OrderPayDetailVO {
    @ApiModelProperty("订单id")
    private Long orderId;

    @ApiModelProperty("用户id")
    private Long userId;

    @ApiModelProperty("订单总金额(分)")
    private Integer totalFee;

    @ApiModelProperty("支付方式")
    private Integer paymentType;

    @ApiModelProperty("订单状态")
    private Integer orderStatus;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;

    @ApiModelProperty("收货地址")
    private AddressDTO address;

    @ApiModelProperty("订单商品明细")
    private List<OrderItemDetailVO> details;
}
