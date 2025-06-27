package com.hidreamai.community.job;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社区任务调度服务启动类
 * 
 * @author hidream
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.hidreamai.community.infra.dal")
@SpringBootApplication
public class CommunityJobApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CommunityJobApplication.class, args);
    }
} 