package com.hmall.item.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BrowseHistoryVO {
    private Long id;
    private Long userId;
    private Long itemId;
    private String itemName;
    private String itemImage;
    private Integer itemPrice;
    private LocalDateTime createTime;
}
