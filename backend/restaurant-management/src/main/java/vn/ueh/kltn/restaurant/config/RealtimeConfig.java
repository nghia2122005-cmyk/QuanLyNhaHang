package vn.ueh.kltn.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RealtimeConfig {
    @Bean(defaultCandidate = false)
    public ThreadPoolTaskExecutor sseExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("sse-send-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(256);
        executor.setDaemon(true);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        // Default AbortPolicy rejects rather than sending on the caller's HTTP thread.
        return executor;
    }
}
