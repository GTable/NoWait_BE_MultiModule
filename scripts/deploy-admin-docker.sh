#!/bin/bash

REPOSITORY="/home/ubuntu/spring-github-action-admin-docker"
cd $REPOSITORY

START_LOG="$REPOSITORY/start.log"

echo "1. find container id"
CONTAINER_ID=$(docker ps -q --filter "name=nowait-app-admin-api")
CONTAINER_PROMETHEUS_ID=$(docker ps -aq --filter "name=prometheus-admin")
CONTAINER_GRAFANA_ID=$(docker ps -aq --filter "name=grafana-admin")

echo "2. stop container"
if [ -n "$CONTAINER_ID" ]; then
  echo "Stopping container $CONTAINER_ID"
  docker rm -f "$CONTAINER_ID"
else
  echo "No admin container found."
fi

echo "Cleaning up old containers…"
docker-compose -f docker-compose.admin.yml -f docker-compose.admin-monitoring.yml -p nowait_dev_admin down

echo "3. start container"
sudo docker-compose -f docker-compose.admin.yml -f docker-compose.admin-monitoring.yml -p nowait_dev_admin pull nowait-app-admin-api
sudo docker-compose -f docker-compose.admin.yml -f docker-compose.admin-monitoring.yml -p nowait_dev_admin up -d nowait-app-admin-api

echo "4. check container status"
NEW_CONTAINER_ID=$(docker ps -q --filter "name=nowait-app-admin-api")
NOW=$(date +%c)

echo "[$NOW] > Container ID: $NEW_CONTAINER_ID"
