package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
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
 * @author 温柔的烟火
 * @date 2024/10/23-11:46
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouterLoader {

    private final RouteDefinitionWriter writer;
    private final NacosConfigManager nacosConfigManager;
    private final String dataId="gateway-routes.json";
    private final String group="DEFAULT_GROUP";
    private final Set<String> routeIds=new HashSet<>();

    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        //项目启动先拉去一次配置，并且添加配置监听器
        String configInfo=nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                //2见听到配置变更，更新路由表
                updateConfigInfo(configInfo);
            }
        });
        //第一次读取到配置更新路由表
        updateConfigInfo(configInfo);
    }

    public void updateConfigInfo(String configInfo){
        log.debug("监听到路由配置信息：{}",configInfo);
        //todo
        //解析配置信息，转为routedefinition
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        //删除旧的路由表
        for (String routeId : routeIds) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        //更新路由
        for (RouteDefinition routeDefinition : routeDefinitions) {

            writer.save(Mono.just(routeDefinition)).subscribe();
            //记录路由id,便于下一次更新的删除
            routeIds.add(routeDefinition.getId());
        }

    }


}
