# Default rules for variable values that configure the Java server.
# It is intended to be executed at the end of env.sh, hence after the settings from an environment file
# and the user data have been applied. It checks various variables for their
# presence, and if no value is set for a variable after evaluating the base env.sh,
# the optional environment appended from http://releases.sapsailing.com/environments,
# and the optional user data from the EC2 environment, default values may be computed
# which may use other variable values that *have* been set.
if [ -z "${SERVER_NAME}" ]; then
  SERVER_NAME=MASTER
fi
if [ -n "${AUTO_REPLICATE}" ]; then
  if [ -z "${REPLICATE_ON_START}" ]; then
    # To start replication upon startup provide the fully-qualified names of the Replicable service classes
    # for which to trigger replication. If you activate this make sure to
    # set the REPLICATE_MASTER_EXCHANGE_NAME variable to the
    # same channel the master is using in its REPLICATION_CHANNEL variable
    REPLICATE_ON_START=com.sap.sailing.server.impl.RacingEventServiceImpl,com.sap.sse.security.impl.SecurityServiceImpl,com.sap.sse.filestorage.impl.FileStorageManagementServiceImpl,com.sap.sse.mail.impl.MailServiceImpl,com.sap.sailing.polars.impl.PolarDataServiceImpl,com.sap.sailing.domain.racelogtracking.impl.fixtracker.RegattaLogFixTrackerRegattaListener,com.sap.sailing.windestimation.integration.WindEstimationFactoryServiceImpl,com.sap.sailing.shared.server.impl.SharedSailingDataImpl,com.sap.sse.landscape.aws.impl.AwsLandscapeStateImpl,com.sap.sailing.domain.igtimiadapter.server.riot.impl.RiotServerImpl
  fi
fi
# Message Queue hostname where to
# send messages for replicas (this server is master)
if [ -z "${REPLICATION_HOST}" ]; then
  REPLICATION_HOST=rabbit.internal.sapsailing.com
fi
# For the port, use 0 for the RabbitMQ default or a specific port that your RabbitMQ server is listening on
if [ -z "${REPLICATION_PORT}" ]; then
  REPLICATION_PORT=0
fi
# The name of the message queuing fan-out exchange that this server will use in its role as replication master.
# Make sure this is unique so that no other master is writing to this exchange at any time.
if [ -z "${REPLICATION_CHANNEL}" ]; then
  if [ -n "${AUTO_REPLICATE}" ]; then
    # This seems to be a replica; use a dedicated outbound channel for "transitive replication"
    REPLICATION_CHANNEL=${SERVER_NAME}-${INSTANCE_NAME}
  else
    # This seems to be a master (or at best a replica only regarding SecurityService / SharedSailingData). User server name
    # as the outbound replication exchange name:
    REPLICATION_CHANNEL=${SERVER_NAME}
  fi
fi

if [ -z "${TELNET_PORT}" ]; then
  TELNET_PORT=14888
fi
if [ -z "${SERVER_PORT}" ]; then
  SERVER_PORT=8888
fi
if [ -z "${MONGODB_NAME}" ]; then
  if [ -n "${AUTO_REPLICATE}" ]; then
    # This seems to be a replica; use a "phony" DB for all replicas:
    MONGODB_NAME=${SERVER_NAME}-replica
  else
    # This seems to be a master
    MONGODB_NAME=${SERVER_NAME}
  fi
fi
if [ -z "${MONGODB_PORT}" ]; then
  MONGODB_PORT=27017
fi
if [ -z "${MONGODB_HOST}" -a -z "${MONGODB_URI}" ]; then
  if [ -n "${AUTO_REPLICATE}" ]; then
    # An auto-replication replica by default assumes it has a local MongoDB replica set running on localhost,
    # called "replica" and running on the default port 27017:
    MONGODB_URI="mongodb://localhost/${MONGODB_NAME}?replicaSet=replica&retryWrites=true&readPreference=nearest"
  else
    MONGODB_URI="mongodb://mongo0.internal.sapsailing.com,mongo1.internal.sapsailing.com/${MONGODB_NAME}?replicaSet=live&retryWrites=true&readPreference=nearest"
  fi
fi
if [ -z "${EXPEDITION_PORT}" ]; then
  EXPEDITION_PORT=2010
fi
if [ -z "${REPLICATE_MASTER_SERVLET_HOST}" ]; then
  REPLICATE_MASTER_SERVLET_HOST=${SERVER_NAME}.sapsailing.com
fi
if [ -z "${REPLICATE_MASTER_SERVLET_PORT}" ]; then
  REPLICATE_MASTER_SERVLET_PORT=443
fi
# Host where RabbitMQ is running 
if [ -z "${REPLICATE_MASTER_QUEUE_HOST}" ]; then
  REPLICATE_MASTER_QUEUE_HOST=rabbit.internal.sapsailing.com
fi
# Port that RabbitMQ is listening on (normally something like 5672); use 0 to connect to RabbitMQ's default port
if [ -z "${REPLICATE_MASTER_QUEUE_PORT}" ]; then
  REPLICATE_MASTER_QUEUE_PORT=0
fi
# Exchange name that the master from which to auto-replicate is using as
# its REPLICATION_CHANNEL variable, mapping to the master's replication.exchangeName
# system property.
#
if [ -z "${REPLICATE_MASTER_EXCHANGE_NAME}" ]; then
  REPLICATE_MASTER_EXCHANGE_NAME=${SERVER_NAME}
fi
if [ -z "${BUILD_COMPLETE_NOTIFY}" ]; then
  export BUILD_COMPLETE_NOTIFY=axel.uhl@sap.com
fi
if [ -z "${SERVER_STARTUP_NOTIFY}" ]; then
  export SERVER_STARTUP_NOTIFY=axel.uhl@sap.com
fi
if [ -z "${MEMORY}" ]; then
  # Compute a default amount of memory based on available physical RAM, with a minimum of 2GB:
  MINIMUM_MEMORY_IN_MB=2000
  MEM_TOTAL=`cat /proc/meminfo  | grep MemTotal | awk '{print $2;}'`
  MEMORY_FOR_APPLICATIONS=$(( ${MEM_TOTAL} / 1024 * 3 / 4 - 1500 / 1 ))
  if [ -z "${TOTAL_MEMORY_SIZE_FACTOR}" ]; then
    # All of the MEMORY_FOR_APPLICATIONS goes to this application
    MEMORY_PER_INSTANCE_IN_MB=$(( ${MEMORY_FOR_APPLICATIONS} < ${MINIMUM_MEMORY_IN_MB} ? ${MINIMUM_MEMORY_IN_MB} : ${MEMORY_FOR_APPLICATIONS} ))
  else
    # Only a fraction (1/${TOTAL_MEMORY_SIZE_FACTOR}) goes to this application:
    MEMORY_PER_INSTANCE_IN_MB=$(( ${MEMORY_FOR_APPLICATIONS} / ${TOTAL_MEMORY_SIZE_FACTOR} < ${MINIMUM_MEMORY_IN_MB} ? ${MINIMUM_MEMORY_IN_MB} : ${MEMORY_FOR_APPLICATIONS} / ${TOTAL_MEMORY_SIZE_FACTOR} ))
  fi
  export MEMORY="${MEMORY_PER_INSTANCE_IN_MB}m"
fi
if [ -n "${CHARGEBEE_SITE}" -a -n "${CHARGEBEE_APIKEY}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dchargebee.site=${CHARGEBEE_SITE} -Dchargebee.apikey=${CHARGEBEE_APIKEY}"
fi
if [ -n "${MANAGE2SAIL_ACCESS_TOKEN}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dmanage2sail.accesstoken=${MANAGE2SAIL_ACCESS_TOKEN}"
fi
if [ -n "${IGTIMI_BASE_URL}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Digtimi.base.url=${IGTIMI_BASE_URL}"
fi
if [ -n "${IGTIMI_BEARER_TOKEN}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Digtimi.bearer.token=${IGTIMI_BEARER_TOKEN}"
fi
if [ -n "${IGTIMI_RIOT_PORT}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Digtimi.riot.port=${IGTIMI_RIOT_PORT}"
fi
if [ -n "${GOOGLE_MAPS_AUTHENTICATION_PARAMS}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dgoogle.maps.authenticationparams=${GOOGLE_MAPS_AUTHENTICATION_PARAMS}"
fi
if [ -n "${YOUTUBE_API_KEY}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dyoutube.api.key=${YOUTUBE_API_KEY}"
fi
if [ -n "${WINDESTIMATION_BEARER_TOKEN}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dwindestimation.source.bearertoken=${WINDESTIMATION_BEARER_TOKEN}"
fi
if [ -n "${POLAR_DATA_BEARER_TOKEN}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dpolardata.source.bearertoken=${POLAR_DATA_BEARER_TOKEN}"
fi
if [ -n "${GEONAMES_ORG_USERNAMES}" ]; then
  ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dgeonames.org.usernames=${GEONAMES_ORG_USERNAMES}"
fi
