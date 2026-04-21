package com.hmall.item.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "商品评论展示对象")
public class ItemCommentVO {

    @ApiModelProperty("评论id")
    private Long id;

    @ApiModelProperty("用户id")
    private Long userId;

    @ApiModelProperty("商品id")
    private Long itemId;

    @ApiModelProperty("商品名称")
    private String itemName;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("评论内容")
    private String content;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
