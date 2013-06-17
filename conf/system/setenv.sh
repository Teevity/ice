# Determine the max heap size based on the instance type.
# See: http://aws.amazon.com/ec2/instance-types/
#
# For nodes in the datacenter we rely on the convention that the hostname
# indicates the amount of memory.
function heapSize {
    instanceType=$(hostname)
    if [ "$EC2_INSTANCE_TYPE" != "" ]; then
        instanceType="$EC2_INSTANCE_TYPE"
    fi

    case $instanceType in
        m1.small)    echo   "1g" ;; # 1.7GB
        m1.large)    echo   "6g" ;; # 7.5GB
        m1.xlarge)   echo  "12g" ;; # 15GB
        t1.micro)    echo "256m" ;; # 613MB
        m2.xlarge)   echo  "14g" ;; # 17.1GB
        m2.2xlarge)  echo  "30g" ;; # 34.2GB
        m2.4xlarge)  echo  "60g" ;; # 68.4GB
        c1.medium)   echo   "1g" ;; # 1.7GB
        c1.xlarge)   echo   "4g" ;; # 7GB
        cc1.4xlarge) echo  "20g" ;; # 23GB
        cg1.4xlarge) echo  "20g" ;; # 22GB
        *-4g*)       echo   "3g" ;;
        *-8g*)       echo   "6g" ;;
        *-16g*)      echo  "14g" ;;
        *-24g*)      echo  "21g" ;;
        *-32g*)      echo  "28g" ;;
        *-64g*)      echo  "60g" ;;
        *-72g*)      echo  "68g" ;;
        *)           echo   "1g" ;; # who knows, try for a gig
    esac
}

# Create the java heap size options. We always set min and max heap size to
# be the same, this ensures that the program will crash right away if there
# is not enough memory.
function javaOptsHeapSize {
    permPercent=$1
    max=$(heapSize)
    permSize="$(echo "$max" | awk "{print int(\$1 * $permPercent)}")g"
    echo "-Xms$max -Xmx$max -XX:PermSize=$permSize -XX:MaxPermSize=$permSize"
}

GCLOG=/apps/tomcat/logs/gc.log
CATALINA_PID=/apps/tomcat/logs/catalina.pid
JRE_HOME=/apps/java
JAVA_HOME=$JRE_HOME
if [ "$1" == "start" ]; then
 JAVA_OPTS=" \
 -javaagent:/apps/appagent/javaagent.jar \
 -verbosegc \
 -XX:+PrintGCDetails \
 -XX:+PrintGCTimeStamps \
 -Xloggc:$GCLOG \
 $(javaOptsHeapSize 0.1) \
 -Xss8192k \
 -Dnetflix.logging.realtimetracers=false \
 -Dlog4j.appender.RTA=org.apache.log4j.varia.NullAppender \
 -Dlog4j.appender.CHUKWA=org.apache.log4j.varia.NullAppender \
 -Dlog4j.appender.CHUKWA.threshold=OFF \
 -Dlog4j.logger.net.spy.memcached.internal.OperationFuture=FATAL \
 -Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.Log4JLogger \
 -Dnetflix.environment=test
 -Dcom.sun.management.jmxremote.ssl=false \
 -Dcom.sun.management.jmxremote.port=7500 \
 -Dcom.sun.management.jmxremote.authenticate=false \
 -Dnetflix.appinfo.name=${NETFLIX_APP}"

 logrotate -f /apps/tomcat/bin/.logrotate.conf
else
 JAVA_OPTS=""
fi
