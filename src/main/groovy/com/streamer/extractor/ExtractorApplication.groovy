package com.streamer.extractor

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.task.TaskExecutor
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@SpringBootApplication
@EnableJpaRepositories
@EnableAsync
class ExtractorApplication {

    @Value('${service.threads}')
    Integer numberOfThreads
    @Value('${service.table-threads}')
    Integer numberOfTableThreads

    static void main(String[] args) {
        SpringApplication.run(ExtractorApplication.class, args)
    }

    @Bean
    TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor()
        executor.setCorePoolSize(numberOfThreads)
        executor.setMaxPoolSize(200000)
        executor.setQueueCapacity(200000)
        return executor
    }

    @Bean
    TaskExecutor tableExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor()
        executor.setCorePoolSize(numberOfTableThreads)
        executor.setMaxPoolSize(200000)
        executor.setQueueCapacity(200000)
        return executor
    }

}
