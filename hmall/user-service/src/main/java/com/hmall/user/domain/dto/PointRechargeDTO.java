package com.hmall.user.domain.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class PointRechargeDTO {

    @NotNull(message = "充值积分不能为空")
    @Min(value = 1, message = "充值积分必须大于0")
    private Integer amount;

    private String remark;
}
