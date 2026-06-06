#!/bin/bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/checkin_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false'
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD='pafunya@520wuerwu'
'
cd /root/bighomework
nohup java -jar target/campus-checkin-assistant-1.0-SNAPSHOT.jar --server.port=8088 > /tmp/check.log 2>&1 &
echo 
