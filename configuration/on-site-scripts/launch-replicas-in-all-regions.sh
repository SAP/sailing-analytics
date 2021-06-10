#!/bin/bash
TARGET_GROUP_NAME=S-ded-tokyo2020

if [ $# -eq 0 ]; then
    echo "$0 -R <release-name> -b <replication-bearer-token> [-t <instance-type>] [-i <ami-id>] [-k <key-pair-name>]"
    echo ""
    echo "-b replication bearer token; mandatory"
    echo "-i Amazon Machine Image (AMI) ID to use to launch the instance; defaults to latest image tagged with image-type:sailing-analytics-server"
    echo "-k Key pair name, mapping to the --key-name parameter"
    echo "-R release name; must be provided to select the release, e.g., build-202106040947"
    echo "-t Instance type; defaults to ${INSTANCE_TYPE}"
    echo
    echo "Example: $0 -b 098toyw098typ9e8/87t9shytp98894y5= -R build-202106041327 -k Jan"
    echo
    echo "Will launch as many new replicas in regions $( cat `dirname $0`/regions.txt ) with the release specified with -R"
    echo "as there are currently healthy auto-replicas registered with teh S-ded-tokyo2020 target group in the region (at least one)."
    echo "which will register at the master proxy tokyo-ssh.internal.sapsailing.com:8888 and RabbitMQ at"
    echo "rabbit-ap-northeast-1.sapsailing.com:5672, then when healthy get added to target group S-ded-tokyo2020"
    echo "in that region, with all auto-replicas registered before removed from the target group."
    exit 2
fi
options='R:b:t:i:k:'
while getopts $options option
do
    case $option in
        b) BEARER_TOKEN=$OPTARG;;
        i) IMAGE_ID=$OPTARG;;
        k) KEY_NAME=$OPTARG;;
        R) RELEASE=$OPTARG;;
        t) INSTANCE_TYPE=$OPTARG;;
        \?) echo "Invalid option"
            exit 4;;
    esac
done
for REGION in $( cat `dirname $0`/regions.txt ); do
  export AWS_DEFAULT_REGION=${REGION}
  echo "Starting replica upgrade process for region ${REGION}"
  echo "-------------------------------------------------------"
  TARGET_GROUP_ARN=$( aws elbv2 describe-target-groups --names ${TARGET_GROUP_NAME} | jq -r '.TargetGroups[].TargetGroupArn' )
  HEALTHY_TARGETS_IN_REGION=$( aws elbv2 describe-target-health --target-group-arn ${TARGET_GROUP_ARN} | jq '.TargetHealthDescriptions | map(select(.TargetHealth.State == "healthy")) | length' )
  echo "Found ${HEALTHY_TARGETS_IN_REGION} healthy targets in target group ${TARGET_GROUP_NAME} in region."
  if [ ${HEALTHY_TARGETS_IN_REGION} = 0 ]; then
    echo "Launching at least one replica."
    HEALTHY_TARGETS_IN_REGION=1
  else
    echo "Launching ${HEALTHY_TARGETS_IN_REGION} new replica(s)."
  fi
  if [ "${REGION}" = "eu-west-1" ]; then
    MONGODB_PRIMARY="mongo0.internal.sapsailing.com:27017,mongo1.internal.sapsailing.com:27017,dbserver.internal.sapsailing.com:10203"
    MONGODB_REPLICA_SET="live"
    VPC_NAME="Default"
  else
    MONGODB_PRIMARY="localhost"
    MONGODB_REPLICA_SET="replica"
    VPC_NAME="Tokyo2020"
  fi
  echo "Using MongoDB primary ${MONGODB_PRIMARY} and replica set ${MONGODB_REPLICA_SET}"
  OPTIONS="-g ${REGION} -b ${BEARER_TOKEN} -R ${RELEASE} -p ${MONGODB_PRIMARY} -r ${MONGODB_REPLICA_SET} -v ${VPC_NAME} -c ${HEALTHY_TARGETS_IN_REGION}"
  if [ -n "${IMAGE_ID}" ]; then
    OPTIONS="${OPTIONS} -i ${IMAGE_ID}"
  fi
  if [ -n "${KEY_NAME}" ]; then
    OPTIONS="${OPTIONS} -k ${KEY_NAME}"
  fi
  if [ -n "${INSTANCE_TYPE}" ]; then
    OPTIONS="${OPTIONS} -t ${INSTANCE_TYPE}"
  fi
  echo "Invoking launch-replicas-in-region.sh with options ${OPTIONS}"
  `dirname $0`/launch-replicas-in-region.sh ${OPTIONS} 
done

