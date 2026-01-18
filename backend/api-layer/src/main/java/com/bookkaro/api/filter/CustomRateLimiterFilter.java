package com.bookkaro.api.filter;

import com.bookkaro.api.dto.RateLimitResponse;
import com.bookkaro.api.service.RateLimiterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Component
public class CustomRateLimiterFilter implements GlobalFilter, Ordered {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private Environment env;

    @Autowired
    private ApplicationContext context;

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
        int rate = env.getProperty("ratelimit." + serviceNode + ".replenishRate", Integer.class, 10);
        int capacity = env.getProperty("ratelimit." + serviceNode + ".burstCapacity", Integer.class, 10);

        Scheduler dedicatedPool = getSchedulerForRoute(serviceNode);

        return rateLimiterService.isAllowed(serviceNode + ":" + path, rate, capacity, serviceNode)
                // Subscribes on the specific pool as received
                .subscribeOn(dedicatedPool)
                .flatMap(isAllowed -> {
                    if (isAllowed) {
                        System.out.println("[RESULT: ALLOWED] Forwarding to backend for: " + path);
                        return chain.filter(exchange);
                    } else {
                        System.out.println("[RESULT: DENIED] 429 Too Many Requests for: " + path);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

                        // This will tell the client that we are sending the response for the failure case too
                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                        RateLimitResponse errorBody = new RateLimitResponse(1, "429 Too Many requests", "Quota exceeded for " + serviceNode, path, 1);

                        try {
                            byte[] bytes = new ObjectMapper().writeValueAsBytes(errorBody);

                            // Wrapping into DataBuffer because WebFlux understands this format
                            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

                            // Writing the buffer
                            return exchange.getResponse().writeWith(Mono.just(buffer));
                        } catch (JsonProcessingException e) {
                            return exchange.getResponse().setComplete();
                        }
                    }
                });
    }

    private Scheduler getSchedulerForRoute(String serviceNode) {
        try {
            return context.getBean(serviceNode + "Scheduler", Scheduler.class);
        } catch (Exception e) {
            return Schedulers.parallel();
        }
    }

    @Override
    public int getOrder() {
//        -1  -->  Highest priority
        return -1;
    }
}
