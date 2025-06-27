# Community Job Service Makefile

.PHONY: help build run test clean docker-build docker-run docker-stop

# 默认目标
help:
	@echo "Community Job Service 构建和部署工具"
	@echo ""
	@echo "可用命令:"
	@echo "  build        - 编译项目"
	@echo "  run          - 本地运行服务"
	@echo "  test         - 运行测试"
	@echo "  clean        - 清理构建文件"
	@echo "  docker-build - 构建 Docker 镜像"
	@echo "  docker-run   - 使用 Docker Compose 启动服务"
	@echo "  docker-stop  - 停止 Docker 服务"
	@echo "  docker-logs  - 查看 Docker 日志"

# 编译项目
build:
	@echo "编译 Community Job Service..."
	mvn clean compile package -DskipTests

# 本地运行服务
run:
	@echo "启动 Community Job Service..."
	mvn spring-boot:run

# 运行测试
test:
	@echo "运行测试..."
	mvn test

# 清理构建文件
clean:
	@echo "清理构建文件..."
	mvn clean

# 构建 Docker 镜像
docker-build:
	@echo "构建 Docker 镜像..."
	docker build -t community-job:latest .

# 使用 Docker Compose 启动服务
docker-run:
	@echo "启动 Docker 服务..."
	docker-compose -f deploy/docker-compose.yml up -d

# 停止 Docker 服务
docker-stop:
	@echo "停止 Docker 服务..."
	docker-compose -f deploy/docker-compose.yml down

# 查看 Docker 日志
docker-logs:
	@echo "查看 Docker 日志..."
	docker-compose -f deploy/docker-compose.yml logs -f community-job

# 重启 Docker 服务
docker-restart:
	@echo "重启 Docker 服务..."
	docker-compose -f deploy/docker-compose.yml restart community-job

# 进入容器
docker-shell:
	@echo "进入容器..."
	docker exec -it community-job /bin/sh

# 查看服务状态
status:
	@echo "检查服务状态..."
	@curl -s http://localhost:8081/community-job/api/job/health || echo "服务未运行"

# 完整部署流程
deploy: clean build docker-build docker-run
	@echo "部署完成！"
	@echo "服务地址: http://localhost:8081/community-job"
	@echo "健康检查: http://localhost:8081/community-job/api/job/health" 