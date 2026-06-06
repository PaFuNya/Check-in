#!/bin/bash
set -e

# ============================================================
#  Campus Check-in System — Deploy Script
#  Steps: Redis check → Frontend build → Backend build → Restart
# ============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[deploy]${NC} $*"; }
warn() { echo -e "${YELLOW}[warn]${NC} $*"; }
err()  { echo -e "${RED}[error]${NC} $*"; }

# ----------------------------------------------------------
# Step 1/5: Check Redis
# ----------------------------------------------------------
log "[1/5] Checking Redis container..."
REDIS_CONTAINER="1Panel-redis-checkin"

if docker ps --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
    log "  Redis container is running."
elif docker ps -a --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
    warn "  Redis container exists but stopped. Starting..."
    docker start "$REDIS_CONTAINER"
    sleep 2
    log "  Redis container started."
else
    err "  Redis container '$REDIS_CONTAINER' not found!"
    err "  Please create it first:"
    err "    docker run -d --name $REDIS_CONTAINER -p 6379:6379 redis:7-alpine"
    exit 1
fi

# Verify Redis connectivity
if command -v redis-cli &>/dev/null; then
    if redis-cli -h 127.0.0.1 -p 6379 ping 2>/dev/null | grep -q PONG; then
        log "  Redis connectivity verified (PONG)."
    else
        warn "  Redis container running but cannot connect. Continuing anyway..."
    fi
else
    # Fallback: check via docker exec
    if docker exec "$REDIS_CONTAINER" redis-cli ping 2>/dev/null | grep -q PONG; then
        log "  Redis connectivity verified via docker exec (PONG)."
    else
        warn "  Could not verify Redis connectivity. Continuing anyway..."
    fi
fi

# ----------------------------------------------------------
# Step 2/5: Build Frontend
# ----------------------------------------------------------
log "[2/5] Building frontend..."
if [ -d "frontend" ] && [ -f "frontend/package.json" ]; then
    cd frontend
    if [ ! -d "node_modules" ]; then
        log "  Installing frontend dependencies..."
        npm install --legacy-peer-deps
    fi
    npm run build
    cd "$PROJECT_DIR"
    if [ -d "frontend/dist" ]; then
        log "  Frontend built successfully → frontend/dist/"
    else
        err "  Frontend build failed: frontend/dist/ not found!"
        exit 1
    fi
else
    warn "  frontend/package.json not found, skipping frontend build."
fi

# ----------------------------------------------------------
# Step 3/5: Build Backend
# ----------------------------------------------------------
log "[3/5] Building backend (mvn clean package -DskipTests)..."
mvn clean package -DskipTests -q

JAR_FILE="target/campus-checkin-assistant-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    err "  Backend build failed: $JAR_FILE not found!"
    exit 1
fi
log "  Backend built successfully → $JAR_FILE"

# ----------------------------------------------------------
# Step 4/5: Restart Backend Service
# ----------------------------------------------------------
log "[4/5] Restarting backend service on port 8088..."

# Stop existing process
if pgrep -f "campus-checkin-assistant" >/dev/null 2>&1; then
    log "  Stopping existing backend process..."
    pkill -f "campus-checkin-assistant" || true
    sleep 3
fi

# Start new process
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/checkin_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false'
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD='pafunya@520wuerwu'

nohup java -jar "$JAR_FILE" \
    --server.port=8088 \
    > /tmp/campus-checkin.log 2>&1 &

BACKEND_PID=$!
log "  Backend started with PID: $BACKEND_PID"

# ----------------------------------------------------------
# Step 5/5: Health Check
# ----------------------------------------------------------
log "[5/5] Waiting for backend to be ready..."
for i in $(seq 1 15); do
    if curl -sf http://localhost:8088/ > /dev/null 2>&1; then
        log "  Backend is ready! (http://localhost:8088)"
        break
    fi
    if [ "$i" -eq 15 ]; then
        warn "  Backend may still be starting. Check logs: tail -f /tmp/campus-checkin.log"
    fi
    sleep 2
done

echo ""
log "=========================================="
log "  Deployment complete!"
log "  Backend:  http://localhost:8088"
log "  Frontend: frontend/dist/"
log "  Logs:     /tmp/campus-checkin.log"
log "=========================================="
