#!/bin/bash
# ============================================
# 校园寝室自助签到系统 — 一键部署脚本
# 用法: ./deploy.sh
# ============================================
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGE_NAME="campus-checkin"
CONTAINER_NAME="campus-checkin"
MYSQL_CONTAINER="1Panel-mysql-RIP6"
DB_NAME="checkin_db"
DB_NETWORK="1panel-network"
APP_PORT="8088"

echo "=========================================="
echo "  校园寝室自助签到系统 — 自动部署"
echo "=========================================="

# 1. 检查 MySQL 容器
echo ""
echo "[1/6] 检查 MySQL 容器..."
if ! docker ps --format '{{.Names}}' | grep -q "$MYSQL_CONTAINER"; then
    echo "  错误: MySQL 容器 $MYSQL_CONTAINER 未运行"
    echo "  请先在 1Panel 中启动 MySQL 容器"
    exit 1
fi
echo "  MySQL 容器运行中 ✓"

# 2. 创建数据库
echo ""
echo "[2/6] 确保数据库存在..."
docker exec "$MYSQL_CONTAINER" mysql -uroot -p'pafunya@520wuerwu' \
    -e "CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>/dev/null
echo "  数据库 $DB_NAME 就绪 ✓"

# 3. 停止旧容器
echo ""
echo "[3/6] 清理旧容器..."
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker stop "$CONTAINER_NAME" 2>/dev/null || true
    docker rm "$CONTAINER_NAME" 2>/dev/null || true
    echo "  旧容器已清理 ✓"
else
    echo "  无需清理 ✓"
fi

# 4. 构建镜像
echo ""
echo "[4/6] 构建 Docker 镜像（首次约 3-5 分钟）..."
cd "$PROJECT_DIR"
docker build -t "$IMAGE_NAME:latest" .
echo "  镜像构建完成 ✓"

# 5. 创建环境变量文件
echo ""
echo "[5/6] 配置环境变量..."
cat > /tmp/checkin.env << EOF
SPRING_DATASOURCE_URL=jdbc:mysql://${MYSQL_CONTAINER}:3306/${DB_NAME}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=pafunya@520wuerwu
JAVA_OPTS=-Xmx256m -Xms128m
EOF
echo "  环境变量配置完成 ✓"

# 6. 启动容器
echo ""
echo "[6/6] 启动容器（端口 $APP_PORT）..."
docker run -d \
    --name "$CONTAINER_NAME" \
    --network "$DB_NETWORK" \
    -p "$APP_PORT:8080" \
    --env-file /tmp/checkin.env \
    --restart unless-stopped \
    "$IMAGE_NAME:latest"
echo "  容器启动完成 ✓"

# 等待启动
echo ""
echo "等待应用启动..."
for i in $(seq 1 30); do
    if curl -sf "http://localhost:$APP_PORT/login" > /dev/null 2>&1; then
        echo ""
        echo "=========================================="
        echo "  部署成功！"
        echo "  访问地址: http://localhost:$APP_PORT"
        echo "  容器名称: $CONTAINER_NAME"
        echo "  查看日志: docker logs -f $CONTAINER_NAME"
        echo "  重启应用: docker restart $CONTAINER_NAME"
        echo "  停止应用: docker stop $CONTAINER_NAME"
        echo "=========================================="
        rm -f /tmp/checkin.env
        exit 0
    fi
    printf "."
    sleep 2
done

echo ""
echo "  警告: 应用启动超时，请检查日志:"
echo "  docker logs $CONTAINER_NAME"
rm -f /tmp/checkin.env
exit 1
