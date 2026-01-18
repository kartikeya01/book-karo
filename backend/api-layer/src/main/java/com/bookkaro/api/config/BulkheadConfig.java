package com.bookkaro.api.config;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class BulkheadConfig {

    @Bean
    public Scheduler paymentServiceScheduler(@Value("${bulkhead.paymentService.poolSize:10}") int size) {
        System.out.println("[CONFIG] Initializing Bulkhead for Payment: " + size + " threads");
        return Schedulers.fromExecutor(Executors.newFixedThreadPool(size, new NamedThreadFactory("paymentService")));
    }

    @Bean
    public Scheduler bookingServiceScheduler(@Value("${bulkhead.bookingService.poolSize:20}") int size) {
        System.out.println("[CONFIG] Initializing Bulkhead for Booking: " + size + " threads");
        return Schedulers.fromExecutor(Executors.newFixedThreadPool(size, new NamedThreadFactory("bookingService")));
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String baseName;
        private final AtomicInteger threadNum = new AtomicInteger(1);

        public NamedThreadFactory(String baseName) { this.baseName = baseName; }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, baseName + "-thread-" + threadNum.getAndIncrement());
        }
    }

}
