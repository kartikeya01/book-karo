package com.bookkaro.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

    /**
     * Configures a Reactive Template for String keys and values.
     * We use StringRedisSerializer because our Rate Limiting keys and
     * token counts are stored as simple strings in Redis.
     */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        System.out.println("[CONFIG] Initializing ReactiveRedisTemplate...");

        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(serializer)
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    /**
     * Loads the Lua script from the resources folder.
     * We expect the script to return a List (e.g., [1, 10] for [allowed, new_count])
     */
    @Bean
    public RedisScript<List> rateLimitScript() {
        System.out.println("[CONFIG] Loading Rate Limiter Lua Script...");

        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("request_rate_limiter.lua"));
        script.setResultType(List.class);
        return script;
    }
}