package com.hmall.gateway.filters;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

/**
 * ClassName: AuthGlobalFilter
 * Package: com.hmall.gateway.filters
 * Description:
 *
 * @Author liang
 * @Create 2024/10/14 15:18
 * @Version jdk17.0
 */
@RequiredArgsConstructor
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher antPathMatcher;
    private  final AuthProperties authProperties;
    private  final JwtTool jwtTool;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求
        ServerHttpRequest request = exchange.getRequest();
        if(isExclude(request.getPath().toString())){
            return  chain.filter(exchange);
        }
        //获取token
        String token =null;
        List<String> authorization = request.getHeaders().get("authorization");
        if(authorization!=null && !authorization.isEmpty()){
            token=authorization.get(0);
        }
        //解析token
        Long userId =null;
        try {
             userId = jwtTool.parseToken(token);
        }catch (UnauthorizedException e){
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return  response.setComplete();
        }
        //传递用户信息
        String userinfo =userId.toString();
        ServerWebExchange serverWebExchange = exchange.
                mutate().request(builder -> builder.header("user-info", userinfo)).build();
        return  chain.filter(serverWebExchange);

    }

    private boolean isExclude(String path) {
        for(String stringPattern : authProperties.getExcludePaths()){
            if(antPathMatcher.match(stringPattern, path)){
                return true;
            }
        }
        return  false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
