package com.hmall.gateway.routes;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * ClassName: DynamicRouteLoader
 * Package: com.hmall.gateway.routes
 * Description:
 *
 * @Author liang
 * @Create 2024/10/15 19:56
 * @Version jdk17.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicRouteLoader {
    private final NacosConfigManager nacosConfigManager;
    private final String dataId ="gateway-routes.json";
    private final  String groupId="DEFAULT_GROUP";

    private final RouteDefinitionWriter writer;
    private final Set<String> routeIds = new HashSet<String>();
    @PostConstruct
    public void initRoutesConfigListener() throws  Exception {
        String configInfo = nacosConfigManager.
                getConfigService().getConfigAndSignListener(dataId, groupId, 5000,new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        updateConfigInfo(configInfo);
                    }
                });

        updateConfigInfo(configInfo);

    }

    private void updateConfigInfo(String configInfo) {
        log.debug("监听到路由配置信息:{}",configInfo);
        //解析配置文件，转换为RouteDefinition
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);

        //删除旧的路由表
        for(String routeId: routeIds){
            writer.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();

        //更新路由表
        for (RouteDefinition routeDefinition : routeDefinitions) {
            writer.save(Mono.just(routeDefinition)).subscribe();
            routeIds.add(routeDefinition.getId());

        }


    }

}
