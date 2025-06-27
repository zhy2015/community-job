package com.hidreamai.community.job.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 基础设施配置类
 * 确保community-infra模块中的组件被正确扫描
 * 
 * @author hidream
 */
@Configuration
@ComponentScan(basePackages = {
    "com.hidreamai.community.infra.dal",
    "com.hidreamai.community.infra.service",
    "com.hidreamai.community.infra.proxy"
})
public class InfraConfig {
    
} 