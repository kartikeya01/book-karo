package com.bookkaro.api.filter;

import com.bookkaro.api.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CustomRateLimiterFilter implements GlobalFilter, Ordered {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println(">>> GATEWAY FILTER ENTERED: " + exchange.getRequest().getURI());
        String path = exchange.getRequest().getPath().value();

        // Fetch the Route ID defined in properties
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String serviceNode = (route != null) ? route.getId() : "default-node";

        System.out.println("[THREAD: " + Thread.currentThread().getName() +
                "] Request received for Service: " + serviceNode +
                " | Path: " + path);

        // Define limits (e.g., 2 requests per 10 seconds) (for testing purpose only) (later to be configured and read from properties file)
        int rate = 2;
        int capacity = 2;

        return rateLimiterService.isAllowed(serviceNode + ":" + path, rate, capacity)
                .flatMap(isAllowed -> {
                    if (isAllowed) {
                        System.out.println("[RESULT: ALLOWED] Forwarding to backend for: " + path);
                        return chain.filter(exchange);
                    } else {
                        System.out.println("[RESULT: DENIED] 429 Too Many Requests for: " + path);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                });
    }

    @Override
    public int getOrder() {
//        -1  -->  Highest priority
        return -1;
    }
}
