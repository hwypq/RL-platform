package org.example.rlplatform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 评测专用线程池
     */
    @Bean("evaluationExecutor")
    public Executor evaluationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();


        executor.setCorePoolSize(2);       
        executor.setMaxPoolSize(4);       
        executor.setQueueCapacity(10);     

        executor.setThreadNamePrefix("eval-");
        executor.setKeepAliveSeconds(60);  // 临时线程空闲60秒后回收


        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());


        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

//        log.info("评测线程池初始化完成: core={}, max={}, queue={}",
//                2, 4, 10);

        return executor;
    }
}
