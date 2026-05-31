#!/bin/bash
# 校园寝室自助签到系统 - 部署脚本
# 使用方法: bash deploy.sh [start|stop|restart|status|build]

APP_NAME="campus-checkin-assistant"
APP_JAR="target/${APP_NAME}-1.0-SNAPSHOT.jar"
PORT=8088
LOG_FILE="/tmp/app.log"
PID_FILE="/tmp/app.pid"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

get_pid() {
    pgrep -f "${APP_JAR}" 2>/dev/null | head -1
}

do_build() {
    echo -e "${YELLOW}[BUILD] 编译打包中...${NC}"
    mvn clean package -DskipTests -q
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[BUILD] 编译成功: ${APP_JAR}${NC}"
    else
        echo -e "${RED}[BUILD] 编译失败!${NC}"
        exit 1
    fi
}

do_start() {
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo -e "${YELLOW}[START] 应用已在运行 (PID: $PID)${NC}"
        return
    fi

    echo -e "${YELLOW}[START] 启动应用 (端口: $PORT)...${NC}"
    nohup java -jar "$APP_JAR" --server.port=$PORT > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"

    # 等待启动
    for i in $(seq 1 30); do
        sleep 1
        HTTP_CODE=$(curl -s --noproxy '*' -o /dev/null -w '%{http_code}' http://127.0.0.1:$PORT/login 2>/dev/null)
        if [ "$HTTP_CODE" = "200" ]; then
            echo -e "${GREEN}[START] 启动成功! PID: $(cat $PID_FILE)${NC}"
            return
        fi
    done
    echo -e "${RED}[START] 启动超时，请检查日志: $LOG_FILE${NC}"
}

do_stop() {
    PID=$(get_pid)
    if [ -z "$PID" ]; then
        echo -e "${YELLOW}[STOP] 应用未运行${NC}"
        return
    fi

    echo -e "${YELLOW}[STOP] 停止应用 (PID: $PID)...${NC}"
    kill "$PID" 2>/dev/null
    sleep 2

    # 强制停止
    if kill -0 "$PID" 2>/dev/null; then
        kill -9 "$PID" 2>/dev/null
    fi

    echo -e "${GREEN}[STOP] 已停止${NC}"
}

do_status() {
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        HTTP_CODE=$(curl -s --noproxy '*' -o /dev/null -w '%{http_code}' http://127.0.0.1:$PORT/login 2>/dev/null)
        echo -e "${GREEN}[STATUS] 运行中 | PID: $PID | HTTP: $HTTP_CODE | 端口: $PORT${NC}"
    else
        echo -e "${RED}[STATUS] 未运行${NC}"
    fi
}

do_restart() {
    do_stop
    sleep 2
    do_start
}

# 主逻辑
case "${1:-restart}" in
    build)    do_build ;;
    start)    do_start ;;
    stop)     do_stop ;;
    restart)  do_build && do_restart ;;
    status)   do_status ;;
    *)
        echo "用法: $0 {build|start|stop|restart|status}"
        exit 1
        ;;
esac
