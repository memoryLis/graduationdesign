package com.hmall.user.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class UserManageVO {
    private Long id;
    private String username;
    private String phone;
    private Integer status;
    private String statusText;
    private Integer points;
    private LocalDateTime createTime;
}
