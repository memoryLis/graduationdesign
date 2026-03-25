package com.hmall.api.client;

import org.apache.ibatis.annotations.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * ClassName: CartClient
 * Package: com.hmall.api.client
 * Description:
 *
 * @Author liang
 * @Create 2024/10/13 14:19
 * @Version jdk17.0
 */
@FeignClient("cart-service")
public interface CartClient {

    @DeleteMapping("/carts")
    public void deleteCartItemByIds(@RequestParam("ids") Collection<Long> ids);
}
