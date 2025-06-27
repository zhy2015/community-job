#!/bin/bash

# Community Job Service 启动脚本

echo "=========================================="
echo "Community Job Service 启动脚本"
echo "=========================================="

# 检查 Java 版本
echo "检查 Java 版本..."
java -version

# 检查 Maven 版本
echo "检查 Maven 版本..."
mvn -version

# 清理并编译
echo "清理并编译项目..."
mvn clean compile package -DskipTests

# 检查编译结果
if [ $? -eq 0 ]; then
    echo "编译成功！"
    echo "启动 Community Job Service..."
    echo "服务地址: http://localhost:8081/community-job"
    echo "健康检查: http://localhost:8081/community-job/api/job/health"
    echo "按 Ctrl+C 停止服务"
    echo "=========================================="
    
    # 启动服务
    mvn spring-boot:run
else
    echo "编译失败，请检查错误信息"
    exit 1
fi 