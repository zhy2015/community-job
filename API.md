# Community Job Service API 文档

## 概述

Community Job Service 提供任务调度和管理功能，包括定时任务执行、任务状态监控和管理员工具接口。

## 基础信息

- **服务地址**: `http://localhost:8081/community-job`
- **API 版本**: v1.0
- **内容类型**: `application/json`

## 通用响应格式

所有 API 响应都使用统一的格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1640995200000
}
```

## 通用接口

### 1. 健康检查

**接口**: `GET /api/job/health`

**描述**: 检查服务健康状态

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "Job服务运行正常",
  "timestamp": 1640995200000
}
```

### 2. 任务状态概览

**接口**: `GET /api/job/status/overview`

**描述**: 获取所有任务的状态概览

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "serviceName": "community-job",
    "timestamp": 1640995200000,
    "version": "1.0.0",
    "likeNotifySync": {
      "taskName": "点赞通知同步任务",
      "isRunning": false,
      "status": "空闲 (连续失败次数: 0)",
      "timestamp": 1640995200000
    }
  },
  "timestamp": 1640995200000
}
```

## 点赞通知同步任务接口

### 1. 获取任务状态

**接口**: `GET /api/job/like-notify/status`

**描述**: 获取点赞通知同步任务的当前状态

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskName": "点赞通知同步任务",
    "isRunning": false,
    "status": "空闲 (连续失败次数: 0)",
    "timestamp": 1640995200000
  },
  "timestamp": 1640995200000
}
```

### 2. 手动触发任务

**接口**: `POST /api/job/like-notify/sync`

**描述**: 手动触发点赞通知同步任务

**请求参数**:
- `startBatch` (可选): 起始批次，默认 0
- `endBatch` (可选): 结束批次，默认 -1（表示处理所有批次）
- `batchSize` (可选): 批量大小，默认 -1（使用自动计算的大小）

**请求示例**:
```bash
curl -X POST "http://localhost:8081/community-job/api/job/like-notify/sync?startBatch=0&endBatch=10&batchSize=200"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "点赞通知同步任务已启动",
  "timestamp": 1640995200000
}
```

### 3. 停止任务

**接口**: `POST /api/job/like-notify/stop`

**描述**: 停止正在运行的点赞通知同步任务

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "点赞通知同步任务已停止",
  "timestamp": 1640995200000
}
```

## 管理员接口

管理员接口提供额外的管理功能，通常需要管理员权限。

### 1. 管理员健康检查

**接口**: `GET /api/admin/job/health`

**描述**: 管理员专用的健康检查接口

**请求参数**:
- `operator` (可选): 操作人标识

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "Job服务运行正常",
  "timestamp": 1640995200000
}
```

### 2. 管理员任务状态概览

**接口**: `GET /api/admin/job/status/overview`

**描述**: 管理员查看所有任务状态概览

**请求参数**:
- `operator` (可选): 操作人标识

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "serviceName": "community-job",
    "timestamp": 1640995200000,
    "version": "1.0.0",
    "likeNotifySync": {
      "taskName": "点赞通知同步任务",
      "isRunning": false,
      "status": "空闲 (连续失败次数: 0)",
      "timestamp": 1640995200000
    }
  },
  "timestamp": 1640995200000
}
```

### 3. 管理员触发点赞通知同步

**接口**: `POST /api/admin/job/like-notify/sync`

**描述**: 管理员手动触发点赞通知同步任务

**请求参数**:
- `startBatch` (可选): 起始批次，默认 0
- `endBatch` (可选): 结束批次，默认 -1
- `batchSize` (可选): 批量大小，默认 -1
- `operator` (可选): 操作人标识

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "管理员已成功触发点赞通知同步任务",
  "timestamp": 1640995200000
}
```

### 4. 管理员停止点赞通知同步

**接口**: `POST /api/admin/job/like-notify/stop`

**描述**: 管理员停止点赞通知同步任务

**请求参数**:
- `operator` (可选): 操作人标识

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "管理员已成功停止点赞通知同步任务",
  "timestamp": 1640995200000
}
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 使用示例

### 1. 检查服务状态

```bash
curl -X GET "http://localhost:8081/community-job/api/job/health"
```

### 2. 查看任务状态

```bash
curl -X GET "http://localhost:8081/community-job/api/job/like-notify/status"
```

### 3. 手动触发任务

```bash
curl -X POST "http://localhost:8081/community-job/api/job/like-notify/sync?startBatch=0&endBatch=5&batchSize=100"
```

### 4. 管理员操作

```bash
# 管理员查看状态
curl -X GET "http://localhost:8081/community-job/api/admin/job/status/overview?operator=admin"

# 管理员触发任务
curl -X POST "http://localhost:8081/community-job/api/admin/job/like-notify/sync?operator=admin&startBatch=0&endBatch=10"
```

## 注意事项

1. 所有时间戳都是毫秒级的 Unix 时间戳
2. 任务执行是异步的，触发后立即返回，实际执行状态需要通过状态接口查询
3. 管理员接口通常需要相应的权限验证
4. 批量参数建议根据数据量大小合理设置，避免内存溢出
5. 服务重启后任务状态会重置 