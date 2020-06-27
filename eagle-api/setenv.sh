#!/bin/bash

# Service name
SERVICE_NAME=eagle-api
#Service path
SERVICE_HOME=/home/eagle/${SERVICE_NAME}
# JDK path
JAVA_HOME="/usr/java/jdk1.8.0_144/"
JAR_PACKAGE="lib/eagle-api.jar"

# dev
#profile=dev
#echo $profile
#if [ "$profile" = "dev" ];then
#    JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home"
#fi

# JVM options
XMS="-Xms4096M"
XMX="-Xmx4096M"
XMN="-Xmn512M"
X_PERM_SIZE="-XX:PermSize=128M"
X_MAX_PERM_SIZE="-XX:MaxPermSize=128M"
X_SERVIVOR_RATIO="-XX:SurvivorRatio=8"