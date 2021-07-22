#!/bin/bash
# Upgrades the entire landscape of servers to a new release ${RELEASE}
# The procedure works in the following steps:
#  - patch *.conf files in sap-p1-1:servers/[master|security_service] and sap-p1-2:servers/[replica|master|security_service] so
#    their INSTALL_FROM_RELEASE points to the new ${RELEASE}
#  - Install new releases to sap-p1-1:servers/[master|security_service] and sap-p1-2:servers/[replica|master|security_service]
#  - Update all launch configurations and auto-scaling groups in the cloud (update-launch-configuration.sh)
#  - Tell all replicas in the cloud to stop replicating (stop-all-cloud-replicas.sh)
#  - Tell sap-p1-2 to stop replicating
#  - on sap-p1-1:servers/master run ./stop; ./start to bring the master to the new release
#  - wait until master is healthy
#  - on sap-p1-2:servers/replica run ./stop; ./start to bring up on-site replica again
#  - launch upgraded cloud replicas and replace old replicas in target group (launch-replicas-in-all-regions.sh)
#  - terminate all instances named "SL Tokyo2020 (auto-replica)"; this should cause the auto-scaling group to launch new instances as required
#  - manually inspect the health of everything and terminate the "SL Tokyo2020 (Upgrade Replica)" instances when enough new instances
#    named "SL Tokyo2020 (auto-replica)" are available
#
KEY_NAME=Axel
INSTANCE_NAME_TO_TERMINATE="SL Tokyo2020 (auto-replica)"
if [ $# -eq 0 ]; then
    echo "$0 -R <release-name> -b <replication-bearer-token> [-t <instance-type>] [-i <ami-id>] [-k <key-pair-name>] [-s]"
    echo ""
    echo "-b replication bearer token; mandatory"
    echo "-i Amazon Machine Image (AMI) ID to use to launch the instance; defaults to latest image tagged with image-type:sailing-analytics-server"
    echo "-k Key pair name, mapping to the --key-name parameter; defaults to Axel"
    echo "-R release name; must be provided to select the release, e.g., build-202106040947"
    echo "-t Instance type; defaults to ${INSTANCE_TYPE}"
    echo "-s Skip release download"
    echo
    echo "Example: $0 -R build-202106041327 -k Jan"
    echo
    echo "Will upgrade the auto-scaling group tokyo2020-* in the regions from regions.txt with a new"
    echo "launch configuration that will be derived from the existing launch configuration named tokyo2020-*"
    echo "by copying it to tokyo2020-{RELEASE_NAME} while updating the INSTALL_FROM_RELEASE parameter in the"
    echo "user data to the {RELEASE_NAME}, and optionally adjuting the AMI, key pair name and instance type if specified."
    echo "Note: this will NOT terminate any instances in the target group!"
    exit 2
fi
options='R:b:t:i:k:s'
while getopts $options option
do
    case $option in
	b) BEARER_TOKEN=$OPTARG;;
        i) IMAGE_ID=$OPTARG;;
        k) KEY_NAME=$OPTARG;;
        R) RELEASE=$OPTARG;;
	s) SKIP_DOWNLOAD=1;;
        t) INSTANCE_TYPE=$OPTARG;;
        \?) echo "Invalid option"
            exit 4;;
    esac
done
RELEASE_FILE=${RELEASE}.tar.gz

function patch_conf_and_install () {
  HOST=$1
  SERVER_DIR=$2
  ssh sailing@$1 "cd servers/${SERVER_DIR}; sed -i -e 's/^INSTALL_FROM_RELEASE=.*$/INSTALL_FROM_RELEASE='${RELEASE}'/' ${SERVER_DIR}.conf; rm env.sh; cat ${SERVER_DIR}.conf | /home/sailing/code/java/target/refreshInstance.sh auto-install-from-stdin"
}

if [ "${SKIP_DOWNLOAD}" = "1" ]; then
  echo " * Skipping download of release file ${RELEASE_FILE}"
else
  echo " * Downloading the release file to sap-p1-1:/home/trac/releases/${RELEASE}"
  ssh sailing@sap-p1-1 "bash --login -c 'mkdir -p /home/trac/releases/${RELEASE}; scp -P 22222 trac@localhost:releases/${RELEASE}/${RELEASE_FILE} /home/trac/releases/${RELEASE}'"
fi
echo " * Patching configurations on sap-p1-1 and sap-p1-2 to new release ${RELEASE} and installing"
patch_conf_and_install sap-p1-1 master
patch_conf_and_install sap-p1-1 security_service
patch_conf_and_install sap-p1-2 replica
patch_conf_and_install sap-p1-2 master
patch_conf_and_install sap-p1-2 security_service
echo " * Updating launch configurations and auto-scaling groups"
OPTIONS="-R ${RELEASE}"
if [ -n "${IMAGE_ID}" ]; then
  OPTIONS="${OPTIONS} -i ${IMAGE_ID}"
fi
if [ -n "${KEY_NAME}" ]; then
  OPTIONS="${OPTIONS} -k ${KEY_NAME}"
fi
if [ -n "${INSTANCE_TYPE}" ]; then
  OPTIONS="${OPTIONS} -t ${INSTANCE_TYPE}"
fi
`dirname $0`/update-launch-configuration.sh ${OPTIONS}
EXIT_CODE=$?
if [ "${EXIT_CODE}" != "0" ]; then
  echo "Updating launch configurations failed with exit code ${EXIT_CODE}."
  exit ${EXIT_CODE}
fi
echo " * Telling all cloud replicas to stop replicating"
`dirname $0`/stop-all-cloud-replicas.sh -b ${BEARER_TOKEN}
EXIT_CODE=$?
if [ "${EXIT_CODE}" != "0" ]; then
  echo "Telling cloud replicas to stop replicating failed with exit code ${EXIT_CODE}"
  exit ${EXIT_CODE}
fi
echo " * Telling replica on sap-p1-2 to stop replicating"
ssh sailing@sap-p1-2 "cd servers/replica; ./stopReplicating.sh ${BEARER_TOKEN}"
EXIT_CODE=$?
if [ "${EXIT_CODE}" != "0" ]; then
  echo "Telling sap-p1-2 replica to stop replicating failed with exit code ${EXIT_CODE}"
  exit ${EXIT_CODE}
fi
echo " * Re-launching master on sap-p1-1 to new release ${RELEASE} and waiting for it to become healthy"
ssh sailing@sap-p1-1 "bash --login -c 'cd servers/master; ./stop; ./start; while ! ./status 2>/dev/null >/dev/null; do echo \"Waiting for healthy master\"; sleep 10; done'"
EXIT_CODE=$?
if [ "${EXIT_CODE}" != "0" ]; then
  echo "Re-launching master on sap-p1-1 failed with exit code ${EXIT_CODE}"
  exit ${EXIT_CODE}
fi
echo " * Re-launching replica on sap-p1-2 to new release ${RELEASE}"
ssh sailing@sap-p1-2 "bash --login -c 'cd servers/replica; ./stop; ./start'"
EXIT_CODE=$?
if [ "${EXIT_CODE}" != "0" ]; then
  echo "Re-launching replica on sap-p1-2 failed with exit code ${EXIT_CODE}"
  exit ${EXIT_CODE}
fi
echo " * Launching upgraded replicas SL Tokyo2020 (Upgrade Replica) in the regions"
OPTIONS="-b ${BEARER_TOKEN} -R ${RELEASE}"
if [ -n "${IMAGE_ID}" ]; then
  OPTIONS="${OPTIONS} -i ${IMAGE_ID}"
fi
if [ -n "${KEY_NAME}" ]; then
  OPTIONS="${OPTIONS} -k ${KEY_NAME}"
fi
if [ -n "${INSTANCE_TYPE}" ]; then
  OPTIONS="${OPTIONS} -t ${INSTANCE_TYPE}"
fi
`dirname $0`/launch-replicas-in-all-regions.sh ${OPTIONS}
EXIT_CODE=$?
if [ "${EXIT_CODE}" != "0" ]; then
  echo "Lanuching replicas in the regions failed with exit code ${EXIT_CODE}"
  exit ${EXIT_CODE}
fi
read -p "Press ENTER to terminate all ${INSTANCE_NAME_TO_TERMINATE} instances"
echo " * Terminating all instances named ${INSTANCE_NAME_TO_TERMINATE} to force auto-scaling group to launch and register upgraded ones"
for REGION in $( cat `dirname $0`/regions.txt ); do
  export AWS_DEFAULT_REGION=${REGION}
  echo "Terminating instances named region ${REGION}"
  echo "-------------------------------------------------------"
  for INSTANCE_ID in $( aws ec2 describe-instances --filters Name=tag:Name,Values="${INSTANCE_NAME_TO_TERMINATE}" | jq -r '.Reservations[].Instances[].InstanceId' ); do
    echo "  Terminating instance ${INSTANCE_ID}"
    aws ec2 terminate-instances --instance-ids ${INSTANCE_ID}
    EXIT_CODE=$?
    if [ "${EXIT_CODE}" != "0" ]; then
      echo "Terminating instance ${INSTANCE_ID} failed with exit code ${EXIT_CODE}"
      exit ${EXIT_CODE}
    fi
  done
done
echo " * DONE"
