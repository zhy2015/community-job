package com.hidreamai.community.job.task;

import com.hidreamai.community.infra.dal.content.dao.RelationDao;
import com.hidreamai.community.infra.dal.content.entity.SocialRelation;
import com.hidreamai.community.infra.enums.biz.RelationTypeEnum;
import com.hidreamai.community.infra.entity.resp.BaseResponse;
import com.hidreamai.community.infra.utils.GsonUtil;
import com.hidreamai.community.notify.entity.request.MessageNotifyReq;
import com.hidreamai.community.notify.proxy.NotifyContentQueryProxy;
import com.hidreamai.community.notify.service.MessageNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 点赞通知同步任务
 * 
 * @author hidream
 */
@Component
@Slf4j
public class LikeNotifySyncJob {
    
    @Resource
    private RelationDao relationDao;
    
    @Resource
    private MessageNotifyService messageNotifyService;
    
    @Resource
    private NotifyContentQueryProxy notifyContentQueryProxy;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // 配置参数
    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final int MAX_BATCH_SIZE = 500;
    private static final int MIN_BATCH_SIZE = 100;
    private static final int CONCURRENT_THREADS = 1;
    private static final int LOG_INTERVAL = 10;
    private static final int NOTIFY_BATCH_SIZE = 20;
    private static final int MAX_RETRY_TIMES = 3;
    private static final int RETRY_DELAY_MS = 3000;
    private static final int MEMORY_CHECK_INTERVAL = 20;
    private static final long MEMORY_THRESHOLD = 800 * 1024 * 1024;
    private static final long SHUTDOWN_TIMEOUT_MS = 30000;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long FAILURE_COOLDOWN_MS = 10000;
    
    // 任务状态跟踪
    private volatile boolean isRunning = false;
    private volatile long lastProcessedPage = -1;
    private volatile long totalProcessedCount = 0;
    private volatile long startTime = 0;
    
    // 应用关闭标志
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private volatile Executor currentExecutor = null;
    
    // 健壮性相关状态
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long lastFailureTime = 0;

    /**
     * 定时执行点赞通知同步任务
     * 每天凌晨2点执行
     */
    public void scheduledSyncLikeNotifications() {
        log.info("定时任务触发点赞通知同步");
        syncLikeNotifications(0, -1, -1);
    }

    /**
     * 应用关闭时的清理方法
     */
    @PreDestroy
    public void onShutdown() {
        log.info("应用正在关闭，设置关闭标志");
        isShuttingDown.set(true);
        
        if (isRunning) {
            log.info("等待当前任务完成，超时时间: {}ms", SHUTDOWN_TIMEOUT_MS);
            long waitStart = System.currentTimeMillis();
            while (isRunning && (System.currentTimeMillis() - waitStart) < SHUTDOWN_TIMEOUT_MS) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (isRunning) {
                log.warn("任务未在超时时间内完成，强制停止");
            } else {
                log.info("任务已正常完成");
            }
        }
        
        if (currentExecutor instanceof ForkJoinPool) {
            ForkJoinPool pool = (ForkJoinPool) currentExecutor;
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 检查应用是否正在关闭
     */
    private boolean isApplicationShuttingDown() {
        try {
            return isShuttingDown.get() || applicationContext.getEnvironment().getActiveProfiles().length == 0;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 检查是否应该跳过执行（基于失败次数）
     */
    private boolean shouldSkipExecution() {
        int failures = consecutiveFailures.get();
        long currentTime = System.currentTimeMillis();
        
        if (failures >= MAX_CONSECUTIVE_FAILURES && 
            (currentTime - lastFailureTime) < FAILURE_COOLDOWN_MS) {
            log.warn("连续失败次数过多({})，跳过本次执行，冷却时间剩余: {}ms", 
                    failures, FAILURE_COOLDOWN_MS - (currentTime - lastFailureTime));
            return true;
        }
        
        return false;
    }

    /**
     * 记录失败并更新状态
     */
    private void recordFailure() {
        consecutiveFailures.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
        log.warn("任务执行失败，连续失败次数: {}", consecutiveFailures.get());
    }

    /**
     * 记录成功并重置失败计数
     */
    private void recordSuccess() {
        consecutiveFailures.set(0);
        log.debug("任务执行成功，重置失败计数");
    }

    /**
     * 异步执行点赞通知同步任务
     */
    @Async("jobTaskExecutor")
    public void syncLikeNotifications(String actualCount) {
        syncLikeNotifications(0, -1, -1);
    }

    /**
     * 带批次范围控制的点赞通知同步任务
     */
    @Async("jobTaskExecutor")
    public void syncLikeNotifications(int startBatch, int endBatch, int batchSize) {
        if (isRunning) {
            log.warn("点赞通知同步任务已在运行中，跳过本次请求");
            return;
        }
        
        if (isApplicationShuttingDown()) {
            log.warn("应用正在关闭，跳过点赞通知同步任务");
            return;
        }
        
        if (shouldSkipExecution()) {
            return;
        }
        
        isRunning = true;
        startTime = System.currentTimeMillis();
        
        try {
            syncLikeNotificationsReal(startBatch, endBatch, batchSize);
            recordSuccess();
        } catch (InterruptedException e) {
            log.warn("点赞通知同步任务被中断");
            Thread.currentThread().interrupt();
        } catch (OutOfMemoryError e) {
            log.error("点赞通知同步任务内存不足", e);
            recordFailure();
            System.gc();
        } catch (Exception e) {
            log.error("点赞通知同步任务执行异常", e);
            recordFailure();
        } catch (Error e) {
            log.error("点赞通知同步任务发生严重错误", e);
            recordFailure();
        } finally {
            isRunning = false;
            log.info("点赞通知同步任务结束，总处理数据量: {}, 最后处理页数: {}, 连续失败次数: {}", 
                    totalProcessedCount, lastProcessedPage, consecutiveFailures.get());
        }
    }

    private void syncLikeNotificationsReal(int startBatch, int endBatch, int batchSize) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        log.info("开始同步点赞通知，startBatch: {}, endBatch: {}, batchSize: {}", 
                startBatch, endBatch, batchSize);
        
        try {
            Long totalCount = relationDao.count(RelationTypeEnum.USER_LIKE_CONTENT.getCode(), null);
            
            if (totalCount == null || totalCount <= 0) {
                log.warn("没有已插入的点赞关系数据需要同步通知，totalCount: {}", totalCount);
                return;
            }

            int calculatedBatchSize = batchSize > 0 ? batchSize : calculateOptimalBatchSize(totalCount);
            int totalBatches = (int) Math.ceil((double) totalCount / calculatedBatchSize);
            
            int actualStartBatch = Math.max(0, startBatch);
            int actualEndBatch = endBatch > 0 ? Math.min(endBatch, totalBatches - 1) : totalBatches - 1;
            int actualTotalBatches = actualEndBatch - actualStartBatch + 1;
            
            if (actualStartBatch > actualEndBatch) {
                log.warn("起始批次大于结束批次，跳过同步: startBatch={}, endBatch={}, totalBatches={}", 
                        startBatch, endBatch, totalBatches);
                return;
            }
            
            log.info("通知同步配置 - 总数据量: {}, 批量大小: {}, 总批次数: {}, 处理批次范围: {}-{}, 实际处理批次数: {}, 并发线程数: {}", 
                    totalCount, calculatedBatchSize, totalBatches, actualStartBatch, actualEndBatch, actualTotalBatches, CONCURRENT_THREADS);

            currentExecutor = new ForkJoinPool(CONCURRENT_THREADS);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            AtomicInteger processedBatches = new AtomicInteger(0);
            AtomicLong processedCount = new AtomicLong(0);
            AtomicInteger successBatches = new AtomicInteger(0);
            AtomicInteger failedBatches = new AtomicInteger(0);

            try {
                for (int pageNo = actualStartBatch; pageNo <= actualEndBatch; pageNo++) {
                    if (isApplicationShuttingDown()) {
                        log.warn("应用正在关闭，停止处理剩余批次");
                        break;
                    }
                    
                    final int currentPageNo = pageNo;
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            if (isApplicationShuttingDown()) {
                                log.debug("应用正在关闭，跳过批次 {}", currentPageNo);
                                return;
                            }
                            
                            List<SocialRelation> batchList = pageQueryLikeRelations(currentPageNo, calculatedBatchSize);
                            
                            if (!batchList.isEmpty()) {
                                boolean success = processBatchNotifySync(batchList);
                                if (success) {
                                    successBatches.incrementAndGet();
                                    lastProcessedPage = currentPageNo;
                                    totalProcessedCount += batchList.size();
                                } else {
                                    failedBatches.incrementAndGet();
                                    log.error("通知同步批次 {} 处理失败", currentPageNo);
                                }
                                processedCount.addAndGet(batchList.size());
                            }
                            
                            batchList.clear();
                            batchList = null;
                            System.gc();
                            
                            int currentProcessed = processedBatches.incrementAndGet();
                            if (currentProcessed % LOG_INTERVAL == 0 || currentProcessed == actualTotalBatches) {
                                log.info("通知同步进度: {}/{} 批次完成, 已处理: {} 条数据, 成功: {} 批次, 失败: {} 批次", 
                                        currentProcessed, actualTotalBatches, processedCount.get(), 
                                        successBatches.get(), failedBatches.get());
                            }
                            
                            if (currentProcessed % MEMORY_CHECK_INTERVAL == 0) {
                                checkAndCleanMemory();
                            }
                        } catch (Exception e) {
                            failedBatches.incrementAndGet();
                            log.error("处理通知同步批次 {} 失败", currentPageNo, e);
                        }
                    }, currentExecutor);
                    
                    futures.add(future);
                }

                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(30, TimeUnit.MINUTES);
                } catch (java.util.concurrent.TimeoutException e) {
                    log.error("通知同步任务超时，已处理: {} 条数据", processedCount.get());
                } catch (InterruptedException e) {
                    log.warn("通知同步任务被中断，已处理: {} 条数据", processedCount.get());
                    futures.forEach(future -> future.cancel(true));
                    throw e;
                } catch (Exception e) {
                    log.error("等待通知同步任务完成时发生异常", e);
                }
            } catch (OutOfMemoryError e) {
                log.error("通知同步任务内存不足", e);
                System.gc();
            } catch (Exception e) {
                log.error("通知同步任务执行异常", e);
            } finally {
                futures.clear();
                System.gc();
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("通知同步完成 - 总耗时: {}ms, 总处理: {} 条数据, 成功: {} 批次, 失败: {} 批次, 平均速度: {} 条/秒", 
                    duration, processedCount.get(), successBatches.get(), failedBatches.get(),
                    processedCount.get() > 0 ? (processedCount.get() * 1000 / duration) : 0);
                    
        } catch (Exception e) {
            log.error("同步任务主流程异常", e);
        }
    }

    /**
     * 分页查询点赞关系数据
     */
    private List<SocialRelation> pageQueryLikeRelations(Integer pageNo, Integer pageSize) {
        for (int retryCount = 0; retryCount <= MAX_RETRY_TIMES; retryCount++) {
            try {
                if (isApplicationShuttingDown()) {
                    log.debug("应用正在关闭，跳过数据库查询");
                    return new ArrayList<>();
                }
                
                return relationDao.pageQueryLikeRelations(
                        RelationTypeEnum.USER_LIKE_CONTENT.getCode(), 
                        pageNo, 
                        pageSize
                );
            } catch (Exception e) {
                if (retryCount == MAX_RETRY_TIMES) {
                    log.error("分页查询点赞关系数据失败，已重试{}次，pageNo: {}, pageSize: {}", 
                            MAX_RETRY_TIMES, pageNo, pageSize, e);
                    return new ArrayList<>();
                }
                
                log.warn("分页查询点赞关系数据失败，第{}次重试，pageNo: {}, pageSize: {}", 
                        retryCount + 1, pageNo, pageSize, e);
                
                try {
                    Thread.sleep(RETRY_DELAY_MS * (retryCount + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new ArrayList<>();
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * 计算最优批量大小
     */
    private int calculateOptimalBatchSize(Long totalCount) {
        if (totalCount <= 10000) {
            return Math.max(MIN_BATCH_SIZE, (int) (totalCount / 50));
        } else if (totalCount <= 100000) {
            return DEFAULT_BATCH_SIZE;
        } else {
            return MAX_BATCH_SIZE;
        }
    }

    /**
     * 批量处理点赞通知同步
     */
    private boolean processBatchNotifySync(List<SocialRelation> batchList) {
        for (int retryCount = 0; retryCount <= MAX_RETRY_TIMES; retryCount++) {
            try {
                return processBatchNotifySyncInternal(batchList);
            } catch (Exception e) {
                if (retryCount == MAX_RETRY_TIMES) {
                    log.error("批量通知同步处理失败，已重试{}次，批次大小: {}, 错误: {}", 
                            MAX_RETRY_TIMES, batchList.size(), e.getMessage());
                    return false;
                }
                
                log.warn("批量通知同步处理失败，第{}次重试，批次大小: {}, 错误: {}", 
                        retryCount + 1, batchList.size(), e.getMessage());
                
                try {
                    Thread.sleep(RETRY_DELAY_MS * (retryCount + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 批量处理点赞通知同步（内部实现）
     */
    private boolean processBatchNotifySyncInternal(List<SocialRelation> batchList) {
        List<MessageNotifyReq> notifyReqs = null;
        try {
            log.debug("开始处理点赞通知同步，批次大小: {}", batchList.size());
            
            int maxBatchSize = Math.min(batchList.size(), 500);
            notifyReqs = new ArrayList<>(maxBatchSize);
            
            for (int i = 0; i < batchList.size(); i++) {
                if (isApplicationShuttingDown()) {
                    log.debug("应用正在关闭，停止处理通知请求");
                    break;
                }
                
                SocialRelation relation = batchList.get(i);
                try {
                    MessageNotifyReq notifyReq = buildLikeNotificationRequest(relation);
                    if (notifyReq != null) {
                        notifyReqs.add(notifyReq);
                    }
                    
                    if ((i + 1) % 100 == 0) {
                        checkAndCleanMemory();
                    }
                } catch (Exception e) {
                    log.warn("构建通知请求失败，sourceId: {}, targetId: {}, error: {}", 
                            relation.getSourceId(), relation.getTargetId(), e.getMessage());
                }
            }
            
            if (!notifyReqs.isEmpty()) {
                processBatchNotify(notifyReqs);
                return true;
            } else {
                log.debug("没有需要同步的通知消息");
                return true;
            }
            
        } catch (OutOfMemoryError e) {
            log.error("通知同步内存不足，批次大小: {}, 错误: {}", batchList.size(), e.getMessage());
            System.gc();
            return false;
        } catch (Exception e) {
            log.error("批量通知同步处理失败，批次大小: {}, 错误: {}", batchList.size(), e.getMessage());
            return false;
        } finally {
            if (notifyReqs != null) {
                notifyReqs.clear();
                notifyReqs = null;
            }
            System.gc();
        }
    }

    /**
     * 构建点赞通知请求
     */
    private MessageNotifyReq buildLikeNotificationRequest(SocialRelation relation) {
        try {
            if (relation == null || relation.getSourceId() == null || relation.getTargetId() == null) {
                log.warn("关系数据不完整，跳过: {}", GsonUtil.toJson(relation));
                return null;
            }
            
            if (isApplicationShuttingDown()) {
                log.debug("应用正在关闭，跳过构建通知请求");
                return null;
            }
            
            String contentOwnerId = notifyContentQueryProxy.queryUserIdByContentId(relation.getTargetId());
            if (contentOwnerId == null) {
                log.debug("无法查询到内容所有者，contentId: {}", relation.getTargetId());
                return null;
            }
            
            if (Objects.equals(relation.getSourceId(), contentOwnerId)) {
                return null;
            }
            
            MessageNotifyReq req = new MessageNotifyReq();
            req.setUserId(contentOwnerId);
            req.setRelateUserId(relation.getSourceId());
            req.setRelateType(RelationTypeEnum.USER_LIKE_CONTENT.getCode());
            req.setContentId(relation.getTargetId());
            if (relation.getCreateTime() != null) {
                req.setNotifyTime(relation.getCreateTime().getTime());
            }
            
            return req;
        } catch (Exception e) {
            log.error("构建点赞通知请求失败，relation: {}", GsonUtil.toJson(relation), e);
            return null;
        }
    }

    /**
     * 批量处理通知
     */
    private void processBatchNotify(List<MessageNotifyReq> notifyReqs) {
        if (notifyReqs.isEmpty()) {
            return;
        }
        
        int totalCount = notifyReqs.size();
        int successCount = 0;
        int failCount = 0;
        
        log.debug("开始批量处理通知，总数: {}", totalCount);
        
        try {
            for (int i = 0; i < totalCount; i += NOTIFY_BATCH_SIZE) {
                if (isApplicationShuttingDown()) {
                    log.debug("应用正在关闭，停止处理通知");
                    break;
                }
                
                int endIndex = Math.min(i + NOTIFY_BATCH_SIZE, totalCount);
                List<MessageNotifyReq> batch = notifyReqs.subList(i, endIndex);
                
                for (MessageNotifyReq req : batch) {
                    try {
                        BaseResponse<Boolean> response = messageNotifyService.notify(req);
                        if (response != null && response.respIsSuccess() && Boolean.TRUE.equals(response.getData())) {
                            successCount++;
                        } else {
                            failCount++;
                            log.warn("通知发送失败，req: {}", GsonUtil.toJson(req));
                        }
                    } catch (Exception e) {
                        failCount++;
                        log.error("通知发送异常，req: {}", GsonUtil.toJson(req), e);
                    }
                }
                
                if ((i + NOTIFY_BATCH_SIZE) % (NOTIFY_BATCH_SIZE * 5) == 0 || endIndex == totalCount) {
                    log.debug("通知处理进度: {}/{} 完成", endIndex, totalCount);
                }
                
                if (endIndex < totalCount) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                if ((i / NOTIFY_BATCH_SIZE + 1) % 5 == 0) {
                    checkAndCleanMemory();
                }
            }
        } catch (OutOfMemoryError e) {
            log.error("批量处理通知内存不足，总数: {}, 错误: {}", totalCount, e.getMessage());
            System.gc();
        } catch (Exception e) {
            log.error("批量处理通知异常，总数: {}, 错误: {}", totalCount, e.getMessage());
        } finally {
            System.gc();
        }
        
        log.info("批量通知处理完成，总数: {}, 成功: {}, 失败: {}", totalCount, successCount, failCount);
    }
    
    /**
     * 获取任务状态
     */
    public String getTaskStatus() {
        if (!isRunning) {
            return String.format("空闲 (连续失败次数: %d)", consecutiveFailures.get());
        }
        
        long currentTime = System.currentTimeMillis();
        long runningTime = currentTime - startTime;
        
        return String.format("运行中 (已运行: %d分钟, 已处理: %d条数据, 最后处理页数: %d, 连续失败次数: %d)", 
                runningTime / 60000, totalProcessedCount, lastProcessedPage, consecutiveFailures.get());
    }
    
    /**
     * 检查任务是否正在运行
     */
    public boolean isTaskRunning() {
        return isRunning;
    }

    /**
     * 检查并清理内存
     */
    private void checkAndCleanMemory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            log.debug("内存使用情况 - 已使用: {}MB, 最大可用: {}MB, 使用率: {}%", 
                    usedMemory / 1024 / 1024, 
                    maxMemory / 1024 / 1024,
                    (usedMemory * 100) / maxMemory);
            
            if (usedMemory > MEMORY_THRESHOLD) {
                log.warn("内存使用超过阈值，执行垃圾回收");
                System.gc();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.warn("检查内存使用情况失败", e);
        }
    }
} 