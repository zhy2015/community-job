package com.hidreamai.community.job.admin;

import com.hidreamai.community.infra.entity.resp.BaseResponse;
import com.hidreamai.community.job.service.JobManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 * 提供管理员专用的任务管理接口
 * 
 * @author hidream
 */
@RestController
@RequestMapping("/api/admin/job")
@Slf4j
public class AdminController {

    @Autowired
    private JobManageService jobManageService;

    /**
     * 管理员手动触发点赞通知同步任务
     */
    @PostMapping("/like-notify/sync")
    public BaseResponse<String> adminTriggerLikeNotifySync(@RequestParam(defaultValue = "0") int startBatch,
                                                          @RequestParam(defaultValue = "-1") int endBatch,
                                                          @RequestParam(defaultValue = "-1") int batchSize,
                                                          @RequestParam(required = false) String operator) {
        try {
            log.info("管理员触发点赞通知同步任务，操作人: {}, 参数: startBatch={}, endBatch={}, batchSize={}", 
                    operator, startBatch, endBatch, batchSize);
            jobManageService.triggerLikeNotifySync(startBatch, endBatch, batchSize);
            return BaseResponse.success("管理员已成功触发点赞通知同步任务");
        } catch (Exception e) {
            log.error("管理员触发点赞通知同步任务失败，操作人: {}", operator, e);
            return BaseResponse.fail(-1,"触发任务失败: " + e.getMessage());
        }
    }

    /**
     * 管理员获取点赞通知同步任务状态
     */
    @GetMapping("/like-notify/status")
    public BaseResponse<Map<String, Object>> adminGetLikeNotifyStatus(@RequestParam(required = false) String operator) {
        try {
            log.info("管理员查询点赞通知同步任务状态，操作人: {}", operator);
            Map<String, Object> status = jobManageService.getLikeNotifyStatus();
            return BaseResponse.success(status);
        } catch (Exception e) {
            log.error("管理员获取点赞通知同步任务状态失败，操作人: {}", operator, e);
            return BaseResponse.fail(-1,"获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 管理员停止点赞通知同步任务
     */
    @PostMapping("/like-notify/stop")
    public BaseResponse<String> adminStopLikeNotifySync(@RequestParam(required = false) String operator) {
        try {
            log.info("管理员停止点赞通知同步任务，操作人: {}", operator);
            jobManageService.stopLikeNotifySync();
            return BaseResponse.success("管理员已成功停止点赞通知同步任务");
        } catch (Exception e) {
            log.error("管理员停止点赞通知同步任务失败，操作人: {}", operator, e);
            return BaseResponse.fail(-1,"停止任务失败: " + e.getMessage());
        }
    }

    /**
     * 管理员获取所有任务状态概览
     */
    @GetMapping("/status/overview")
    public BaseResponse<Map<String, Object>> adminGetJobStatusOverview(@RequestParam(required = false) String operator) {
        try {
            log.info("管理员查询任务状态概览，操作人: {}", operator);
            Map<String, Object> overview = jobManageService.getJobStatusOverview();
            return BaseResponse.success(overview);
        } catch (Exception e) {
            log.error("管理员获取任务状态概览失败，操作人: {}", operator, e);
            return BaseResponse.fail(-1,"获取概览失败: " + e.getMessage());
        }
    }

    /**
     * 管理员健康检查接口
     */
    @GetMapping("/health")
    public BaseResponse<String> adminHealth(@RequestParam(required = false) String operator) {
        log.info("管理员健康检查，操作人: {}", operator);
        return BaseResponse.success("Job服务运行正常");
    }
} 