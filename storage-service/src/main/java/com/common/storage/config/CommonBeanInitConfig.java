package com.common.storage.config;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CommonBeanInitConfig {

    @Value("${storage.executor.core_pool_size:2}")
    private int corePoolSize;
    @Value("${storage.executor.max_pool_size:4}")
    private int maxPoolSize;
    @Value("${storage.executor.queue_capacity:100}")
    private int queueCapacity;

    @Bean
    Tika tika() {
        return new Tika();
    }

    @Bean(name = "mediaTaskExecutor")
    TaskExecutor mediaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("storage-media-");
        executor.setCorePoolSize(this.corePoolSize);
        executor.setMaxPoolSize(this.maxPoolSize);
        executor.setQueueCapacity(this.queueCapacity);
        executor.initialize();
        return executor;
    }
}
