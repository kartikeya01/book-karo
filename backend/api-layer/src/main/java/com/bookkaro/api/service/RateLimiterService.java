package com.bookkaro.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class RateLimiterService {

    @Autowired
    @Qualifier("reactiveRedisTemplate") // Resolves the '2 beans found' error
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisScript<List> script;

    /**
     * Checks with Redis if a request is allowed for a given key.
     */
    public Mono< Boolean> isAllowed(String key, int replenishRate, int burstCapacity, String serviceNode) {
        // Unique keys for the token count and the last refill timestamp
        List<String> keys = Arrays.asList(key + ":tokens", key + ":timestamp");

        // Arguments: [ReplenishRate, BurstCapacity, CurrentTimestamp]
        String now = String.valueOf(Instant.now().getEpochSecond());
        List<String> args = Arrays.asList(
                String.valueOf(replenishRate),
                String.valueOf(burstCapacity),
                now
        );

        System.out.println("[SERVICE] Checking Redis for Key: " + key +
                " | Limits: " + replenishRate + " TPS / " + burstCapacity + " Burst for service " + serviceNode);

        return redisTemplate.execute(script, keys, args)
                .next()
                .map(result -> {
                    // Result[0] is 'allowed' (1L or 0L)
                    // Result[1] is 'new tokens remaining'
                    Long allowed = (Long) result.get(0);
                    Long remaining = (Long) result.get(1);

                    if (allowed == 1L) {
                        System.out.println("[REDIS RESPONSE] ALLOWED. Remaining Tokens: " + remaining);
                        return true;
                    } else {
                        System.out.println("[REDIS RESPONSE] DENIED. Out of tokens.");
                        return false;
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("[REDIS ERROR] " + e.getMessage() + ". Defaulting to ALLOW (Fail-Open).");
                    return Mono.just(true);
                });
    }
}