package com.hidreamai.community.job.controller;

import com.hidreamai.community.infra.entity.resp.BaseResponse;
import com.hidreamai.community.job.service.JobManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 任务管理控制器
 * 
 * @author hidream
 */
@RestController
@RequestMapping("/capi/job")
@Slf4j
public class JobController {

    @Autowired
    private JobManageService jobManageService;

    /**
     * 手动触发点赞通知同步任务
     */
    @PostMapping("/like-notify/sync")
    public BaseResponse<String> triggerLikeNotifySync(@RequestParam(defaultValue = "0") int startBatch,
                                                     @RequestParam(defaultValue = "-1") int endBatch,
                                                     @RequestParam(defaultValue = "-1") int batchSize) {
        try {
            log.info("手动触发点赞通知同步任务，startBatch: {}, endBatch: {}, batchSize: {}", startBatch, endBatch, batchSize);
            jobManageService.triggerLikeNotifySync(startBatch, endBatch, batchSize);
            return BaseResponse.success("点赞通知同步任务已启动");
        } catch (Exception e) {
            log.error("触发点赞通知同步任务失败", e);
            return BaseResponse.fail(-1, "触发任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取点赞通知同步任务状态
     */
    @GetMapping("/like-notify/status")
    public BaseResponse<Map<String, Object>> getLikeNotifyStatus() {
        try {
            Map<String, Object> status = jobManageService.getLikeNotifyStatus();
            return BaseResponse.success(status);
        } catch (Exception e) {
            log.error("获取点赞通知同步任务状态失败", e);
            return BaseResponse.fail(-1, "获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 停止点赞通知同步任务
     */
    @PostMapping("/like-notify/stop")
    public BaseResponse<String> stopLikeNotifySync() {
        try {
            log.info("手动停止点赞通知同步任务");
            jobManageService.stopLikeNotifySync();
            return BaseResponse.success("点赞通知同步任务已停止");
        } catch (Exception e) {
            log.error("停止点赞通知同步任务失败", e);
            return BaseResponse.fail(-1,"停止任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有任务状态概览
     */
    @GetMapping("/status/overview")
    public BaseResponse<Map<String, Object>> getJobStatusOverview() {
        try {
            Map<String, Object> overview = jobManageService.getJobStatusOverview();
            return BaseResponse.success(overview);
        } catch (Exception e) {
            log.error("获取任务状态概览失败", e);
            return BaseResponse.fail(-1,"获取概览失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return BaseResponse.success("Job服务运行正常");
    }
} 