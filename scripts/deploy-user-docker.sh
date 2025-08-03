#!/bin/bash

REPOSITORY="/home/ubuntu/spring-github-action-user-docker"
cd $REPOSITORY

START_LOG="$REPOSITORY/start.log"

echo "1. find container id"
CONTAINER_ID=$(docker ps -q --filter "name=nowait-app-user-api")
CONTAINER_PROMETHEUS_ID=$(docker ps -q --filter "name=prometheus-user")
CONTAINER_GRAFANA_ID=$(docker ps -q --filter "name=grafana-user")

echo "2. stop container"
if [ -n "$CONTAINER_ID" ]; then
  echo "Stopping container $CONTAINER_ID"
  docker rm -f "$CONTAINER_ID"

  echo "Stopping container $CONTAINER_PROMETHEUS_ID"
  docker rm -f "$CONTAINER_PROMETHEUS_ID"

  echo "Stopping container $CONTAINER_GRAFANA_ID"
    docker rm -f "$CONTAINER_GRAFANA_ID"
else
  echo "No user container found."
fi

echo "3. start container"
sudo docker-compose -f docker-compose.user.yml -f docker-compose.user-monitoring.yml -p nowait_dev pull nowait-app-user-api prometheus-user grafana-user
sudo docker-compose -f docker-compose.user.yml -f docker-compose.user-monitoring.yml -p nowait_dev up -d nowait-app-user-api prometheus-user grafana-user

echo "4. check container status"
NEW_CONTAINER_ID=$(docker ps -q --filter "name=nowait-app-user-api")
NOW=$(date +%c)

echo "[$NOW] > Container ID: $NEW_CONTAINER_ID"
