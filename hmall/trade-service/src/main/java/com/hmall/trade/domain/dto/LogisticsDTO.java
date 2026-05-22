package com.hmall.trade.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "物流信息表单")
public class LogisticsDTO {
    @ApiModelProperty("物流公司名称")
    private String logisticsCompany;

    @ApiModelProperty("物流单号")
    private String logisticsNumber;
}
