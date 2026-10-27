package com.treasure.restart.func.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AiStreamConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService aiStreamExecutor(){
        return Executors.newFixedThreadPool(4);
    }
}
