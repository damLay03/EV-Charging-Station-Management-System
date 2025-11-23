package com.swp.evchargingstation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Configuration for async event processing.
 *
 * This enables @Async event listeners to run in background threads,
 * preventing blocking of main business logic.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncEventConfig implements AsyncConfigurer {

    /**
     * Thread pool cho async event listeners.
     *
     * Configuration:
     * - Core pool size: 5 threads
     * - Max pool size: 10 threads
     * - Queue capacity: 100 events
     * - Thread name prefix: "event-" for easy identification in logs
     */
    @Bean(name = "eventExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Initialized async event executor - Core: 5, Max: 10, Queue: 100");
        return executor;
    }

    /**
     * Handle exceptions trong async listeners.
     *
     * Exceptions are logged but don't affect main flow.
     * In production, you should:
     * - Send alerts to monitoring system
     * - Store failed events in dead letter queue for retry
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("❌ Async event listener failed - Method: {}, Error: {}",
                        method.getName(), ex.getMessage(), ex);

                // TODO: Send alert to monitoring system (e.g., Sentry, DataDog)
                // TODO: Store failed event in dead letter queue for manual review/retry
            }
        };
    }
}

