#!/bin/bash

REPOSITORY="/home/ubuntu/spring-github-action-user"
cd $REPOSITORY

APP_NAME=application-user
JAR_NAME=$(ls $REPOSITORY | grep '.jar' | tail -n 1)
JAR_PATH=$REPOSITORY/$JAR_NAME
STOP_LOG="$REPOSITORY/stop.log"

$ chmod 666 "$STOP_LOG"

CURRENT_PID=$(pgrep -f ${APP_NAME}.*.jar)

if [ -z $CURRENT_PID ]
then
  echo "서비스 NouFound" >> $STOP_LOG
else
  echo "> kill -15 $CURRENT_PID"
  kill -15 $CURRENT_PID
  sleep 5
fi
