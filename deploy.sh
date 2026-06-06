#!/bin/bash
set -e

echo "[1/5] Checking Redis..."
docker ps | grep 1Panel-redis-checkin || docker start 1Panel-redis-checkin

echo "[2/5] Building frontend..."
cd frontend && npm run build && cd ..

echo "[3/5] Building backend..."
mvn clean package -DskipTests -q

echo "[4/5] Restarting backend..."
pkill -f campus-checkin || true; sleep 2
nohup java -jar target/campus-checkin-assistant-1.0-SNAPSHOT.jar \
  --server.port=8088 > /tmp/check.log 2>&1 &

echo "[5/5] Done! PID: $!"
