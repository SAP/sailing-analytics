#!/bin/bash
INSTANCE_TYPE=c5.4xlarge
REPLICA_SET_NAME=replica
REPLICA_SET_PRIMARY=localhost
KEY_NAME=Axel
VPC=Paris2024
TARGET_GROUP_NAME=S-paris2024
COUNT=1

if [ $# -eq 0 ]; then
    echo "$0 -g <AWS-region> -R <release-name> -b <replication-bearer-token> -m <google-maps-api-key> [-c <instance-count>] [-r <replica-set-name>] [-p <host>:<port>] [-t <instance-type>] [-i <ami-id>] [-k <key-pair-name>] [-v <VPC name> ]"
    echo ""
    echo "-b replication bearer token; mandatory"
    echo "-c Count; defaults to ${COUNT}"
    echo "-i Amazon Machine Image (AMI) ID to use to launch the instance; defaults to latest image tagged with image-type:sailing-analytics-server"
    echo "-g AWS Region, e.g., eu-west-1"
    echo "-m Google Maps API key, used for the GOOGLE_MAPS_AUTHENTICATION_PARAMS variable"
    echo "-k SSH key pair name, mapping to the --key-name parameter; defaults to Axel"
    echo "-p MongoDB primary host[:port]; defaults to ${REPLICA_SET_PRIMARY}"
    echo "-r MongoDB replica set name; defaults to ${REPLICA_SET_NAME}"
    echo "-R release name; must be provided to select the release, e.g., build-202106040947"
    echo "-t Instance type; defaults to ${INSTANCE_TYPE}"
    echo "-v VPC name; defaults to ${VPC}"
    echo
    echo "Example: $0 -g ap-southeast-2 -b 098toyw098typ9e8/87t9shytp98894y5= -R build-202106041327 -k Jan"
    echo
    echo "Will launch one or more (see -c) new replicas in the AWS region specified with -g  with the release specified with -R"
    echo "which will register at the master proxy paris-ssh.internal.sapsailing.com:8888 and RabbitMQ at"
    echo "rabbit-eu-west-3.sapsailing.com:5672, then when healthy get added to target group S-paris2024"
    echo "in that region, with all auto-replicas registered before removed from the target group."
    echo "Specify -r and -p if you are launching in eu-west-1 because it has a special non-default environment."
    exit 2
fi

options='g:R:b:c:r:p:t:i:k:v:m:'
while getopts $options option
do
    case $option in
	b) BEARER_TOKEN=$OPTARG;;
	c) COUNT=$OPTARG;;
	g) REGION=$OPTARG;;
        i) IMAGE_ID=$OPTARG;;
	k) KEY_NAME=$OPTARG;;
        m) GOOGLE_MAPS_AUTHENTICATION_PARAMS=$OPTARG;;
	p) REPLICA_SET_PRIMARY=$OPTARG;;
	R) RELEASE=$OPTARG;;
	r) REPLICA_SET_NAME=$OPTARG;;
        t) INSTANCE_TYPE=$OPTARG;;
	v) VPC=$OPTARG;;
        \?) echo "Invalid option"
            exit 4;;
    esac
done
export AWS_DEFAULT_REGION=${REGION}
if [ -z "$IMAGE_ID" ]; then
  IMAGE_ID=$( `dirname $0`/../../aws-automation/getLatestImageOfType.sh sailing-analytics-server )
fi
SECURITY_GROUP_ID=$( aws ec2 describe-security-groups --filters Name=tag:Name,Values="Sailing Analytics App" | jq -r '.SecurityGroups[].GroupId' )
echo "Found security group ${SECURITY_GROUP_ID} with name \"Sailing Analytics App\""
VPC_ID=$( aws --region ${REGION} ec2 describe-vpcs --filters Name=tag:Name,Values=${VPC} | jq -r '.Vpcs[].VpcId' )
echo "Found VPC ${VPC_ID}"
SUBNETS=$( aws --region ${REGION} ec2 describe-subnets --filters Name=vpc-id,Values=${VPC_ID} )
NUMBER_OF_SUBNETS=$( echo "${SUBNETS}" | jq -r '.Subnets | length' )
TARGET_GROUP_ARN=$( aws elbv2 describe-target-groups --names ${TARGET_GROUP_NAME} | jq -r '.TargetGroups[].TargetGroupArn' )
echo "Found target group with name ${TARGET_GROUP_NAME} and ARN ${TARGET_GROUP_ARN}"
PRIVATE_IPS=
INSTANCE_IDS=
i=0
while [ ${i} -lt ${COUNT} ]; do
  SUBNET_INDEX=$(( $RANDOM * $NUMBER_OF_SUBNETS / 32768 ))
  SUBNET_ID=$( echo "${SUBNETS}" | jq -r '.Subnets['${SUBNET_INDEX}'].SubnetId' )
  echo "Launching image with ID ${IMAGE_ID} into subnet #${SUBNET_INDEX} in region ${REGION} with ID ${SUBNET_ID} in VPC ${VPC_ID}"
  SECURITY_OPTIONS='-Dsecurity.sharedAcrossSubdomainsOf=sailing.omegatiming.com -Dsecurity.baseUrlForCrossDomainStorage=https://security-service.sapsailing.com -Dgwt.acceptableCrossDomainStorageRequestOriginRegexp=https?://(.*\.)?sailing\.omegatiming\.com(:[0-9]*)?$'
  PRIVATE_IP_AND_INSTANCE_ID=$( aws --region ${REGION} ec2 run-instances --subnet-id ${SUBNET_ID} --instance-type ${INSTANCE_TYPE} --security-group-ids ${SECURITY_GROUP_ID} --image-id ${IMAGE_ID} --user-data "INSTALL_FROM_RELEASE=${RELEASE}
SERVER_NAME=paris2024
MONGODB_URI=\"mongodb://${REPLICA_SET_PRIMARY}/paris2024-replica?replicaSet=${REPLICA_SET_NAME}&retryWrites=true&readPreference=nearest\"
USE_ENVIRONMENT=live-replica-server
REPLICATION_CHANNEL=paris2024-replica
REPLICATION_HOST=rabbit-eu-west-3.sapsailing.com
REPLICATE_MASTER_SERVLET_HOST=paris-ssh.internal.sapsailing.com
REPLICATE_MASTER_SERVLET_PORT=8888
REPLICATE_MASTER_EXCHANGE_NAME=paris2024
REPLICATE_MASTER_QUEUE_HOST=rabbit-eu-west-3.sapsailing.com
REPLICATE_MASTER_BEARER_TOKEN=${BEARER_TOKEN}
GOOGLE_MAPS_AUTHENTICATION_PARAMS=\"${GOOGLE_MAPS_AUTHENTICATION_PARAMS}\"
ADDITIONAL_JAVA_ARGS=\"\${ADDITIONAL_JAVA_ARGS} -Dcom.sap.sse.debranding=true ${SECURITY_OPTIONS}\"" --ebs-optimized --key-name $KEY_NAME --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=SL Paris2024 (Upgrade Replica)},{Key=sailing-analytics-server,Value=paris2024}]" "ResourceType=volume,Tags=[{Key=Name,Value=SL Paris2024 (Upgrade Replica)}]" | jq -r '.Instances[].PrivateIpAddress + " " + .Instances[].InstanceId' )
  EXIT_CODE=$?
  if [ "${EXIT_CODE}" != "0" ]; then
    echo "Error launching instance in region ${REGION}. Exiting with status ${EXIT_CODE}"
    exit ${EXIT_CODE}
  fi
  PRIVATE_IP=$( echo ${PRIVATE_IP_AND_INSTANCE_ID} | awk '{print $1;}' )
  INSTANCE_ID=$( echo ${PRIVATE_IP_AND_INSTANCE_ID} | awk '{print $2;}' )
  PRIVATE_IPS="${PRIVATE_IPS} ${PRIVATE_IP}"
  if [ -z $INSTANCE_IDS ]; then
    INSTANCE_IDS="Id=${INSTANCE_ID}"
  else
    INSTANCE_IDS="${INSTANCE_IDS} Id=${INSTANCE_ID}"
  fi
  # Now wait for those instances launched to become available
  echo "Waiting for instance with private IP ${PRIVATE_IP} in region ${REGION} to become healthy..."
  while ! ssh -A -o StrictHostKeyChecking=no ec2-user@paris-ssh.sapsailing.com "ssh -o StrictHostKeyChecking=no root@${PRIVATE_IP} \"cd /home/sailing/servers/paris2024; ./status >/dev/null\""; do
    echo "${PRIVATE_IP} in region ${REGION} still not healthy. Trying again in 10s..."
    sleep 10
  done
  i=$(( i + 1 ))
done
OLD_VERSION_TARGET_IDS=$( aws elbv2 describe-target-health --target-group-arn ${TARGET_GROUP_ARN} | jq -r '.TargetHealthDescriptions[].Target.Id' )
TARGET_IDS_TO_DEREGISTER=""
for OLD_VERSION_TARGET_ID in ${OLD_VERSION_TARGET_IDS}; do
  if [ -z $TARGET_IDS_TO_DEREGISTER ]; then
    TARGET_IDS_TO_DEREGISTER="Id=${OLD_VERSION_TARGET_ID}"
  else
    TARGET_IDS_TO_DEREGISTER="${TARGET_IDS_TO_DEREGISTER} Id=${OLD_VERSION_TARGET_ID}"
  fi
done
echo "Registering instances ${INSTANCE_IDS} with target group ${TARGET_GROUP_NAME} in region ${REGION}"
aws elbv2 register-targets --target-group-arn ${TARGET_GROUP_ARN} --targets ${INSTANCE_IDS}
EXIT_CODE=$?
if [ "${EXIT_CODE}" = "0" ]; then
  echo "Registering instances in region ${REGION} was successful."
  echo "De-registering old instances ${TARGET_IDS_TO_DEREGISTER} from target group ${TARGET_GROUP_NAME} in region ${REGION}"
  aws elbv2 deregister-targets --target-group-arn ${TARGET_GROUP_ARN} --targets ${TARGET_IDS_TO_DEREGISTER}
else
  echo "Registering instances in region ${REGION} failed with exit code $?; not de-registering old instances."
  exit ${EXIT_CODE}
fi
