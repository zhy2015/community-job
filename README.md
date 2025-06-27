# Community Job Service

社区任务调度服务，专门用于处理后台任务、定时任务和管理员工具接口。

## 功能特性

- **任务调度**: 支持定时任务和异步任务执行
- **任务管理**: 提供任务状态监控和管理接口
- **管理员工具**: 专门的管理员接口，用于系统维护
- **健康检查**: 提供完整的健康检查和监控端点

## 主要任务

### 1. 点赞通知同步任务 (LikeNotifySyncJob)

- **功能**: 同步点赞关系数据到通知系统
- **定时执行**: 每天凌晨2点自动执行
- **手动触发**: 支持管理员手动触发和参数控制
- **状态监控**: 实时监控任务执行状态

### 2. 更多任务待扩展...

## 技术栈

- Spring Boot 2.6.13
- Spring Scheduling
- MyBatis
- Redis
- MySQL

## 快速开始

### 1. 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+

### 2. 配置数据库

确保数据库连接配置正确，参考 `application.yml` 中的配置。

### 3. 启动服务

```bash
# 编译
mvn clean compile

# 运行
mvn spring-boot:run
```

### 4. 访问接口

- 健康检查: `GET http://localhost:8081/community-job/api/job/health`
- 任务状态: `GET http://localhost:8081/community-job/api/job/status/overview`
- 管理员接口: `GET http://localhost:8081/community-job/api/admin/job/health`

## API 接口

### 通用接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/job/health` | GET | 健康检查 |
| `/api/job/status/overview` | GET | 任务状态概览 |
| `/api/job/like-notify/status` | GET | 点赞通知同步任务状态 |
| `/api/job/like-notify/sync` | POST | 手动触发点赞通知同步 |
| `/api/job/like-notify/stop` | POST | 停止点赞通知同步任务 |

### 管理员接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/admin/job/health` | GET | 管理员健康检查 |
| `/api/admin/job/status/overview` | GET | 管理员任务状态概览 |
| `/api/admin/job/like-notify/status` | GET | 管理员查看点赞通知同步状态 |
| `/api/admin/job/like-notify/sync` | POST | 管理员手动触发点赞通知同步 |
| `/api/admin/job/like-notify/stop` | POST | 管理员停止点赞通知同步 |

## 配置说明

### 任务配置

```yaml
job:
  like-notify:
    cron: "0 0 2 * * ?"  # 定时任务表达式
    batch-size: 200      # 批量处理大小
    max-batch-size: 500  # 最大批量大小
    min-batch-size: 100  # 最小批量大小
    concurrent-threads: 1 # 并发线程数
    max-retry-times: 3   # 最大重试次数
    retry-delay-ms: 3000 # 重试延迟
```

### 线程池配置

- **jobTaskExecutor**: 异步任务线程池
- **scheduledTaskExecutor**: 定时任务线程池

## 监控和日志

### 日志级别

- 开发环境: DEBUG
- 生产环境: INFO

### 监控端点

- `/actuator/health`: 健康检查
- `/actuator/info`: 应用信息
- `/actuator/metrics`: 指标监控

## 部署

### Docker 部署

```dockerfile
FROM openjdk:8-jre-alpine
COPY target/community-job.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes 部署

参考 `deploy/manifests/` 目录下的 Kubernetes 配置文件。

## 开发指南

### 添加新任务

1. 在 `task` 包下创建新的任务类
2. 实现任务逻辑
3. 在 `JobManageService` 中添加管理方法
4. 在 `JobController` 和 `AdminController` 中添加接口

### 任务最佳实践

- 使用 `@Async` 注解实现异步执行
- 使用 `@Scheduled` 注解实现定时执行
- 实现优雅关闭机制
- 添加完善的错误处理和重试机制
- 监控内存使用情况

## 故障排查

### 常见问题

1. **任务执行失败**: 检查数据库连接和配置
2. **内存不足**: 调整批量大小和并发线程数
3. **任务重复执行**: 检查定时任务配置

### 日志分析

关键日志关键字：
- `点赞通知同步任务`: 任务执行相关
- `内存使用情况`: 内存监控相关
- `任务执行异常`: 错误处理相关

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交代码
4. 创建 Pull Request

## 许可证

MIT License 