package com.hmall.user.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserPointSummaryVO {
    private Long userId;
    private String username;
    private Integer points;
}
