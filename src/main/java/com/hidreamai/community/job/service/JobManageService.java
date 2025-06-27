package com.hidreamai.community.job.service;

import com.hidreamai.community.job.task.LikeNotifySyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务管理服务
 * 
 * @author hidream
 */
@Service
@Slf4j
public class JobManageService {

    @Autowired
    private LikeNotifySyncJob likeNotifySyncJob;

    /**
     * 触发点赞通知同步任务
     */
    public void triggerLikeNotifySync(int startBatch, int endBatch, int batchSize) {
        log.info("触发点赞通知同步任务，参数: startBatch={}, endBatch={}, batchSize={}", startBatch, endBatch, batchSize);
        likeNotifySyncJob.syncLikeNotifications(startBatch, endBatch, batchSize);
    }

    /**
     * 获取点赞通知同步任务状态
     */
    public Map<String, Object> getLikeNotifyStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("taskName", "点赞通知同步任务");
        status.put("isRunning", likeNotifySyncJob.isTaskRunning());
        status.put("status", likeNotifySyncJob.getTaskStatus());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }

    /**
     * 停止点赞通知同步任务
     */
    public void stopLikeNotifySync() {
        log.info("停止点赞通知同步任务");
        // 注意：这里只是记录日志，实际停止逻辑需要在任务内部实现
        // 由于任务本身有超时和中断处理机制，这里主要提供接口层面的停止信号
    }

    /**
     * 获取所有任务状态概览
     */
    public Map<String, Object> getJobStatusOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 点赞通知同步任务状态
        Map<String, Object> likeNotifyStatus = getLikeNotifyStatus();
        overview.put("likeNotifySync", likeNotifyStatus);
        
        // 服务基本信息
        overview.put("serviceName", "community-job");
        overview.put("timestamp", System.currentTimeMillis());
        overview.put("version", "1.0.0");
        
        return overview;
    }
} 