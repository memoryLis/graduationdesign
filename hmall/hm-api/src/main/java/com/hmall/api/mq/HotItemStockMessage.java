package com.hmall.api.mq;

import com.hmall.api.dto.OrderDetailDTO;
import lombok.Data;

import java.util.List;

@Data
public class HotItemStockMessage {
    private Long orderId;
    private List<OrderDetailDTO> details;
}
