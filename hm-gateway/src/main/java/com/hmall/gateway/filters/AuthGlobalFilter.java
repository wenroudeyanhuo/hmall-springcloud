package com.hmall.gateway.filters;


import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.util.JwtTool;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * @author 温柔的烟火
 * @date 2024/10/22-20:59
 */
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
    private final AntPathMatcher antPathMatcher=new AntPathMatcher();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /*
        获取request对象
        判断是否需要做登录拦截
        获取token
        校验并解析token
        把用户信息保存到请求头，传递用户信息

        放行
         */

        ServerHttpRequest request = exchange.getRequest();
        if (isExclude(request.getPath().toString())){
            //放行
            return chain.filter(exchange);
        }
//        RequestPath path = request.getPath();
//        authProperties.getExcludePaths();
        String token=null;
        List<String> authorization = request.getHeaders().get("authorization");
        if (authorization!=null && !authorization.isEmpty()){
            token=authorization.get(0);
        }
        Long userId=null;
        try {
            userId = jwtTool.parseToken(token);
        }catch (UnauthorizedException e){
            //401 未登录或未授权
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();//到这终止
        }
        String userInfo=userId.toString();
        ServerWebExchange swe = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();
        //放行
        return chain.filter(swe);
    }
    private boolean isExclude(String path){
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern,path)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
