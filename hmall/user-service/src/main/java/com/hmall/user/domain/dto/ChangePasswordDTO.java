package com.hmall.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "修改密码表单实体")
public class ChangePasswordDTO {
    @ApiModelProperty(value = "原密码", required = true)
    @NotNull(message = "原密码不能为空")
    private String oldPassword;

    @ApiModelProperty(value = "新密码", required = true)
    @NotNull(message = "新密码不能为空")
    private String newPassword;
}
