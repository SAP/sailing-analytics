#!/bin/bash

# This file contains the configuration for the Java server

# *******************************************************
# ATTENTION: Make sure to always also check the variables at the end
# of this file as there could be overwritten ones!
# *******************************************************

# Source secrets if available; they may be overwritten in the user data section and by an environment.
if [ -f configuration/secrets ]; then
  . configuration/secrets
fi

# Set the heap size here if you want to override the default which
# will compute a MEMORY assignment from the total
# memory installed in the machine and the number of server instances to
# start on that machine.
# MEMORY="6000m"

# Host / port where the master Java instance is running
# Make sure firewall configurations allow access
#
# REPLICATE_MASTER_SERVLET_HOST=
# REPLICATE_MASTER_SERVLET_PORT=

# Exchange name that the master from which to auto-replicate is using as
# its REPLICATION_CHANNEL variable, mapping to the master's replication.exchangeName
# system property.
#
# REPLICATE_MASTER_EXCHANGE_NAME=

# Credentials for replication access to the master server
# Make sure, the user is granted the permission SERVER:REPLICATE:<server-name>
# Credentials can be provided either as a combination of username and password,
# or alternatively as a single bearer token that was obtained, e.g., through
#   curl -d "username=myuser&password=mysecretpassword" "https://master-server.sapsailing.com/security/api/restsecurity/access_token" | jq .access_token
# or by logging in to the master server using your web browser and then navigating to
#     https://master-server.sapsailing.com/security/api/restsecurity/access_token
# 
# REPLICATE_MASTER_USERNAME=
# REPLICATE_MASTER_PASSWORD=
# REPLICATE_MASTER_BEARER_TOKEN=

# Automatic build and test configuration
BUILD_BEFORE_START=False
BUILD_FROM=master
COMPILE_GWT=True
RUN_TESTS=False
CODE_DIRECTORY=code

# Specify an email adress that should be notified
# whenever a build or install has been completed
#
# BUILD_COMPLETE_NOTIFY=

# Specify an email address that should be notified
# whenever the server has been started
#
# SERVER_STARTUP_NOTIFY=

# Specify filename that is usually located at
# http://release.sapsailing.com/ that should
# be used as a base for the server
#
# INSTALL_FROM_RELEASE=

# Specify name of file that can usually be found
# at http://release.sapsailing.com/environments
#
# USE_ENVIRONMENT=

# Figuring out what the "mail" tool version is so we
# can know whether it requires -a or -A to attach a file:
if which mail; then
    if mail -V | grep -q "GNU Mailutils" 2>/dev/null; then
      MAIL_ATTACH_OPTION="-A"
    else
      MAIL_ATTACH_OPTION="-a"
    fi
fi

INSTANCE_ID="$SERVER_NAME:$SERVER_PORT"
if [[ ! -d $JAVA_HOME ]] && [[ -f "/usr/libexec/java_home" ]]; then
    JAVA_HOME=`/usr/libexec/java_home`
fi
JAVA_BINARY="$JAVA_HOME/bin/java"
JAVA_VERSION_OUTPUT=$("$JAVA_BINARY" -version 2>&1)
JAVA_VERSION=$(echo "$JAVA_VERSION_OUTPUT" | sed 's/^.* version "\(.*\)\.\(.*\)\..*".*$/\1.\2/; 1q')
export JAVA_11_LOGGING_ARGS="-Xlog:gc+ergo*=trace:file=logs/gc_ergo.log:time:filecount=10,filesize=100000000 -Xlog:gc*:file=logs/gc.log:time:filecount=10,filesize=100000000"
export JAVA_11_ARGS="-agentlib:jdwp=transport=dt_socket,address=0.0.0.0:8000,onjcmd=y,server=y -Dosgi.java.profile=file://`pwd`/JavaSE-11.profile --add-modules=ALL-SYSTEM -Djavax.xml.bind.JAXBContextFactory=com.sun.xml.bind.v2.ContextFactory -XX:ThreadPriorityPolicy=1 -XX:+UnlockExperimentalVMOptions ${JAVA_11_LOGGING_ARGS}"
export JAVA_8_LOGGING_ARGS="-XX:+PrintAdaptiveSizePolicy -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -Xloggc:logs/gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"
echo JAVA_VERSION detected: $JAVA_VERSION >&2
JAVA_MAJOR_VERSION=$( echo "${JAVA_VERSION}" | sed -e 's/^\([0-9]*\)\..*$/\1/' )
if [ ${JAVA_MAJOR_VERSION} -ge 11 ]; then
  echo "Java major version >=11 detected ">&2
  JAVA_VERSION_SPECIFIC_ARGS=$JAVA_11_ARGS
else
  echo Java before version 11 detected >&2
  # options for use with SAP JVM only:
  if echo "$JAVA_VERSION_OUTPUT" | grep -q "SAP Java"; then
    ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -XX:+GCHistory -XX:GCHistoryFilename=logs/sapjvm_gc@PID.prf"
    BUILD=$( echo "$JAVA_VERSION_OUTPUT" | grep "(build [^ ]*)" )
    MAJOR=$( echo "$BUILD" | sed -e 's/^.*(build \([0-9]*\)\.\([0-9]*\)\.\([0-9]*\)).*$/\1/' )
    MINOR=$( echo "$BUILD" | sed -e 's/^.*(build \([0-9]*\)\.\([0-9]*\)\.\([0-9]*\)).*$/\2/' )
    UPDATE=$( echo "$BUILD" | sed -e 's/^.*(build \([0-9]*\)\.\([0-9]*\)\.\([0-9]*\)).*$/\3/' )
    echo "SAP JVM $MAJOR $MINOR $UPDATE detected" >&2
    if [ $MAJOR -ge 8 -a $MINOR -ge 1 -a $UPDATE -ge 45 ]; then
      echo "Update 8.1.045 or later; using Java11 GC logging options" >&2
      LOGGING_ARGS="$JAVA_11_LOGGING_ARGS"
    else 
      echo "Update before 8.1.045; using Java8 GC logging options" >&2
      LOGGING_ARGS="$JAVA_8_LOGGING_ARGS"
    fi
  else
    # non-SAP JVM 8
    export LOGGING_ARGS="$JAVA_8_LOGGING_ARGS"
  fi
  export JAVA_8_ARGS="-XX:ThreadPriorityPolicy=2 -XX:+UseG1GC ${LOGGING_ARGS}"
  JAVA_VERSION_SPECIFIC_ARGS=$JAVA_8_ARGS
fi

# White labeling / SAP Branding: use -Dcom.sap.sse.debranding=false to show SAP branding images and text.
# Note: This is allowed only for versions of this software that have been reviewed by SAP, e.g., have come
# from an SAP-controlled Git repository such as github.com/SAP/sailing-analytics. In all other cases, the
# usual restrictions for the use of the SAP brand and logo apply. See also https://brand.sap.com/
#ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dcom.sap.sse.debranding=false"
# Anniversary calculation:
#ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -DAnniversaryRaceDeterminator.enabled=true"
ADDITIONAL_JAVA_ARGS="$JAVA_VERSION_SPECIFIC_ARGS $ADDITIONAL_JAVA_ARGS -Dpersistentcompetitors.clear=false -Drestore.tracked.races=true -XX:MaxGCPauseMillis=500 -Dorg.eclipse.jetty.LEVEL=OFF -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog"
# Use the following to obtain initial polar sheet data from the archive server, without live replication:
#ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dpolardata.source.url=https://www.sapsailing.com"
# Use the following to obtain initial models for wind estimation from maneuvers from the archive server, without live replication:
#ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dwindestimation.source.url=https://www.sapsailing.com"

# Custom event management URL: use -Dcom.sap.sailing.eventmanagement.url to modify from hardcoded default (https://my.sapsailing.com) to, e.g., https://dev.sapsailing.com
#ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dcom.sap.sailing.eventmanagement.url=https://dev.sapsailing.com"

# To enable the use of the shared SecurityService and SharedSailingData from security-service.sapsailing.com, uncomment and fill in the following:
#ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dsecurity.sharedAcrossSubdomainsOf=sapsailing.com -Dsecurity.baseUrlForCrossDomainStorage=https://security-service.sapsailing.com -Dgwt.acceptableCrossDomainStorageRequestOriginRegexp=https?://(.*\.)?sapsailing\.com(:[0-9]*)?$"
#REPLICATE_ON_START=com.sap.sse.security.impl.SecurityServiceImpl,com.sap.sailing.shared.server.impl.SharedSailingDataImpl
#REPLICATE_MASTER_SERVLET_HOST=security-service.sapsailing.com
#REPLICATE_MASTER_SERVLET_PORT=443
#REPLICATE_MASTER_EXCHANGE_NAME=security_service
# Obtain the bearer token for user security-service-replicator by logging on to https://security-service.sapsailing.com and then
# getting https://security-service.sapsailing.com/security/api/restsecurity/access_token
#REPLICATE_MASTER_BEARER_TOKEN="..."

ON_AMAZON=`command -v ec2-metadata`
### End of Standard env.sh ###
