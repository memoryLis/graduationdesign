package com.hmall.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(description = "订单商品明细")
@Data
public class OrderItemDetailVO {
    @ApiModelProperty("商品id")
    private Long itemId;

    @ApiModelProperty("商品名称")
    private String name;

    @ApiModelProperty("规格")
    private String spec;

    @ApiModelProperty("单价(分)")
    private Integer price;

    @ApiModelProperty("购买数量")
    private Integer num;

    @ApiModelProperty("商品图片")
    private String image;
}
