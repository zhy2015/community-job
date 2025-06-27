package com.hidreamai.community.job.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据源配置类
 * 
 * @author hidream
 */
@Configuration
public class DataSourceConfig {
    
    // 数据源配置由application.yml中的spring.datasource配置自动处理
    // 这里可以添加自定义的数据源配置逻辑
    
} 