#!/bin/bash

path=$(cd "$(dirname "$0")"; pwd)
cd $path

chmod +x ./bin/*.sh
source ./bin/setenv.sh

#gc log
GC_LOG="$SERVICE_HOME/logs/gc.log"

#jvm settings
JVM_OPTS="$JVM_OPTS $XMS $XMX $XMN $X_PERM_SIZE $X_MAX_PERM_SIZE $X_SERVIVOR_RATIO -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$GC_LOG -XX:CMSInitiatingOccupancyFraction=40 -XX:+UseCMSInitiatingOccupancyOnly -XX:+CMSClassUnloadingEnabled -XX:+DisableExplicitGC -XX:+PrintPromotionFailure -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$SERVICE_HOME/logs/oom.dump"

DATE=date
DT="$DATE +%Y%m%d%H%M%S"
CT="$DATE +%s"

# check pid exists
psid=0
checkpid() {
   javaps=`ps -ef | grep java | grep $SERVICE_NAME`

   if [ -n "$javaps" ]; then
      psid=`echo $javaps | awk '{print $2}'`
   else
      psid=0
   fi
}

function check()
{
    checkpid

    if [ $psid -ne 0 ]; then
    	mkdir -p $SERVICE_HOME/logs/jstack
        $JSTACK $psid > $SERVICE_HOME/logs/jstack/jstack.log.`$DT`;
        find $SERVICE_HOME/logs/jstack/ -name 'jstack.log.*' -mtime +1 -delete
    else
    	echo "`$DT` check $SERVICE_NAME failed"
    	## TODO: op team deal with failing alert?
        #sendsms "$SERVICE_NAME check failed!"
        ##stop;start;
    fi
}

## start
start() {
   checkpid

   if [ $psid -ne 0 ]; then
      echo "================================"
      echo "warn: $SERVICE_NAME already started! (pid=$psid)"
      echo "================================"
   else
      echo -n "Starting $SERVICE_NAME with JVM_OPTS: $JVM_OPTS..."
	    nohup date > server.out 2>&1 < /dev/null
      exec $JAVA_HOME/bin/java -jar $JVM_OPTS $JAR_PACKAGE >> server.out 2>&1
      checkpid
      if [ $psid -ne 0 ]; then
         echo "(pid=$psid) [OK]"
      else
         echo "[Failed]"
      fi
   fi
}

stop() {

   checkpid
   if [ $psid -ne 0 ]; then
      echo -n "Stopping $SERVICE_NAME ...(pid=$psid) "
      ## shutdown gracefully first
      kill $psid
      count=0
	  while [ $count -lt 20 ]
	  do
   		echo "Sleep 3s to wait server stopping..."
   		sleep 3
   		checkpid
	    if [ $psid -eq 0 ]; then
	    	echo "Server stopped."
	       	break
	    fi
   		count=`expr $count + 1`
	  done

    checkpid
    if [ $psid -ne 0 ]; then
        echo "Shutdown gracefully failed, kill -9."
        kill -9 $psid
      if [ $? -eq 0 ]; then
         echo "[OK]"
      else
         echo "[Failed]"
      fi
    fi
   else
      echo "================================"
      echo "warn: $SERVICE_NAME is not running"
      echo "================================"
   fi
}

info() {
   echo "System Information:"
   echo "****************************"
   echo `head -n 1 /etc/issue`
   echo `uname -a`
   echo
   echo "JAVA_HOME=$JAVA_HOME"
   echo `$JAVA_HOME/bin/java -version`
   echo
   echo "SERVICE_HOME=$SERVICE_HOME"
   echo "SERVICE_NAME=$SERVICE_NAME"
   echo "JAR_PACKAGE=$JAR_PACKAGE"
   echo "****************************"
}

case "$1" in
   'start')
      start
      ;;
   'stop')
     stop
     ;;
   'reload')
     stop
     start
    ;;
   'info')
     info
     ;;
   'check')
     check
     ;;
  *)
     echo "Usage: $0 {start|stop|reload|check|info}"
     exit 1
esac
exit 0