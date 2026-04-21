package com.hmall.api.mq;

import lombok.Data;

import java.util.List;

@Data
public class CartClearMessage {
    private Long userId;
    private List<Long> itemIds;
}
