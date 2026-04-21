package com.hmall.item.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "商品评论新增表单")
public class ItemCommentFormDTO {

    @ApiModelProperty("商品id")
    private Long itemId;

    @ApiModelProperty("评论内容")
    private String content;
}
