#!/bin/bash
ENV_SH="`pwd`/env.sh"
ENV_SH_DEFAULTS="`pwd`/env-default-rules.sh"
if [ -f "${ENV_SH}" ]; then
  chmod a+x "${ENV_SH}"
  source "${ENV_SH}"
fi
if [ -f "${ENV_SH_DEFAULTS}" ]; then
  chmod a+x "${ENV_SH_DEFAULTS}"
  source "${ENV_SH_DEFAULTS}"
fi
ON_AMAZON=`command -v ec2-metadata`
DATE_OF_EXECUTION=`date`

# The following temporary file may be used by this script to dump EC2-provided user data
# variables to it; they can then be sourced from there and later appended to a new env.sh
ec2EnvVars_tmpFile=`mktemp /tmp/ec2EnvVars_XXX`

find_project_home () 
{
    if [[ $1 == '/' ]] || [[ $1 == "" ]]; then
        echo ""
        return 0
    fi
    if [ ! -d "$1/.git" ]; then
        PARENT_DIR=`cd $1/..;pwd`
        OUTPUT=$(find_project_home $PARENT_DIR)
        if [ "$OUTPUT" = "" ] && [ -d "$PARENT_DIR/$CODE_DIRECTORY" ] && [ -d "$PARENT_DIR/$CODE_DIRECTORY/.git" ]; then
            OUTPUT="$PARENT_DIR/$CODE_DIRECTORY"
        fi
        echo $OUTPUT
        return 0
    fi
    echo $1 | sed -e 's/\/cygdrive\/\([a-zA-Z]\)/\1:/'
}

checks ()
{
    USER_HOME=~
    START_DIR=`pwd`
    PROJECT_HOME=$(find_project_home $START_DIR)
    # needed for maven on sapsailing.com to work correctly
    if [ -f $USER_HOME/.bash_profile ]; then
        source $USER_HOME/.bash_profile
    fi
    JAVA_BINARY=$JAVA_HOME/bin/java
    if [[ ! -d "$JAVA_HOME" ]]; then
        echo "Could not find $JAVA_BINARY set in env.sh. Trying to find the correct one..."
	JAVA_VERSION=$(java -version 2>&1 | sed 's/^\(java version "\(.*\)\.\(.*\)\..*"\)\|\(openjdk version "\(.*\)\.\(.*\)\.\(.*\)" .*\)$/\3\5/; 1q')
        if [ "$JAVA_VERSION" -lt 7 -o "$JAVA_VERSION" -gt 8 ]; then
            echo "The current Java version ($JAVA_VERSION) does not match the requirements (>= Java 7, <= Java 8)."
            exit 10
        fi
        JAVA_BINARY=`which java`
        echo "Using Java from $JAVA_BINARY"
    fi
    if [[ $DEPLOY_TO == "" ]]; then
        SERVER_HOME=.
    else
        SERVER_HOME=$USER_HOME/servers/$DEPLOY_TO
    fi
    if [[ ! -d $SERVER_HOME ]]; then
        SERVER_HOME=`pwd`/../../servers/$DEPLOY_TO
        if [[ ! -d $SERVER_HOME ]]; then
            echo "Could not find the correct directory for the server - assumed $SERVER_HOME. Adapt DEPLOY_TO in env.sh to point to the right server directory."
            exit 10
        fi
    fi
}

copy_user_data_to_tmp_file ()
{
    echo "Reading user-data provided by Amazon instance data to ${ec2EnvVars_tmpFile}"
    VARS=$(ec2-metadata -d | sed "s/user-data\: //g")
    if [[ "$VARS" != "not available" ]]; then
        ec2-metadata -d | sed "s/user-data\: //g" >>"${ec2EnvVars_tmpFile}"
    else
        echo "No user data has been provided."
    fi
}

# loads the user data-provided variables by sourcing the script
activate_user_data ()
{
    # make sure to reload data
    source "${ec2EnvVars_tmpFile}"
    INSTANCE_NAME=`ec2-metadata -i | cut -f2 -d " "`
    INSTANCE_IP4=`ec2-metadata -v | cut -f2 -d " "`
    INSTANCE_DNS=`ec2-metadata -p | cut -f2 -d " "`
    INSTANCE_ID="$INSTANCE_NAME ($INSTANCE_IP4)"
}

append_default_envsh_rules()
{
    echo "
# Default rules: START ($DATE_OF_EXECUTION)" >> $SERVER_HOME/env.sh
    cat "${SERVER_HOME}/env-default-rules.sh" >>$SERVER_HOME/env.sh
    echo "
# Default rules: END" >> $SERVER_HOME/env.sh
    echo "Updated env.sh with data from env-default-rules.sh file!"
}

append_user_data_to_envsh ()
{
    mkdir -p $SERVER_HOME/environment 2>/dev/null >/dev/null
    # make backup of original file
    cp $SERVER_HOME/env.sh $SERVER_HOME/environment/env.sh.backup

    echo "
# User-Data: START ($DATE_OF_EXECUTION)" >> $SERVER_HOME/env.sh
    echo "INSTANCE_NAME=`ec2-metadata -i | cut -f2 -d \" \"`" >> $SERVER_HOME/env.sh
    echo "INSTANCE_IP4=`ec2-metadata -v | cut -f2 -d \" \"`" >> $SERVER_HOME/env.sh
    echo "INSTANCE_INTERNAL_IP4=`ec2-metadata -o | cut -f2 -d \" \"`" >> $SERVER_HOME/env.sh
    echo "INSTANCE_DNS=`ec2-metadata -p | cut -f2 -d \" \"`" >> $SERVER_HOME/env.sh
    # Append EC2 user data to env.sh file:
    cat "${ec2EnvVars_tmpFile}" >>$SERVER_HOME/env.sh

    echo "INSTANCE_ID=\"$INSTANCE_NAME ($INSTANCE_IP4)\"" >> $SERVER_HOME/env.sh
    echo "# User-Data: END" >> $SERVER_HOME/env.sh
    echo "Updated env.sh with data from user-data field!"
}

install_environment ()
{
    if [[ $USE_ENVIRONMENT != "" ]]; then
        # clean up directory to really make sure that there are no files left
        rm -rf ${SERVER_HOME}/environment
        mkdir ${SERVER_HOME}/environment
        if [[ ${INSTALL_FROM_SCP_USER_AT_HOST_AND_PORT} != "" ]]; then
            SCP_PORT=$( echo ${INSTALL_FROM_SCP_USER_AT_HOST_AND_PORT} | sed -e 's/^[^:]*:\?\([0-9]*\)\?$/\1/' )
            if [ -n "${SCP_PORT}" ]; then
                SCP_PORT_OPTION="-P ${SCP_PORT}"
            fi
            SCP_HOST=$( echo ${INSTALL_FROM_SCP_USER_AT_HOST_AND_PORT} | sed -e 's/^\([^:]*\):\?\([0-9]*\)\?$/\1/' )
	    echo "Using environment ${SCP_HOST}:/home/trac/releases/environments/${USE_ENVIRONMENT}"
	    mkdir -p ./environment
            scp ${SCP_PORT_OPTION} ${SCP_HOST}:/home/trac/releases/environments/${USE_ENVIRONMENT} ./environment
        else
	    echo "Using environment https://releases.sapsailing.com/environments/$USE_ENVIRONMENT"
	    wget -P environment https://releases.sapsailing.com/environments/$USE_ENVIRONMENT
	fi
        echo "
# Environment ($USE_ENVIRONMENT): START ($DATE_OF_EXECUTION)" >> $SERVER_HOME/env.sh
        cat ${SERVER_HOME}/environment/$USE_ENVIRONMENT >> $SERVER_HOME/env.sh
        echo "
# Environment: END" >> ${SERVER_HOME}/env.sh
        echo "Updated env.sh with data from environment file!"
    else
        echo "No environment file specified!"
    fi
}

load_from_release_file ()
{
    if [[ ${INSTALL_FROM_RELEASE} == "" ]]; then
        INSTALL_FROM_RELEASE="$(wget -O - https://releases.sapsailing.com/ 2>/dev/null | grep main- | tail -1 | sed -e 's/^.*\(main-[0-9]*\).*$/\1/')"
        echo "You didn't provide a release. Defaulting to latest master build https://releases.sapsailing.com/$INSTALL_FROM_RELEASE"
        # Alternatively, to download a release from Github, here is how the latest Java8/main-release can be obtained:
        #   curl -L -H 'Authorization: Bearer ***' https://api.github.com/repos/SAP/sailing-analytics/releases 2>/dev/null | jq -r 'sort_by(.created_at) | reverse | map(select(.name | startswith("main-")))[0].assets[] | select(.content_type=="application/x-tar").id'
        # will output something like:
        #   169233159
        # which can then be used in a request such as:
        #   curl -L -o main-1234567890.tar.gz -H 'Accept: application/octet-stream' -H 'Authorization: Bearer ***' 'https://api.github.com/repos/SAP/sailing-analytics/releases/assets/169233159'
        # Or to obtain the latest docker-17 release, try this:
        #   curl -L -H 'Authorization: Bearer ***' https://api.github.com/repos/SAP/sailing-analytics/releases 2>/dev/null | jq -r 'sort_by(.created_at) | reverse | map(select(.name | startswith("docker-17-")))[0].assets[] | select(.content_type=="application/x-tar").id'
        # and then on like above...
    fi
    if which mail; then
        if [ -n "${BUILD_COMPLETE_NOTIFY}" ]; then
          echo "Build/Deployment process has been started - it can take 5 to 20 minutes until your instance is ready. " | mail -r noreply@sapsailing.com -s "Build or Deployment of $INSTANCE_ID to $SERVER_HOME for server $SERVER_NAME starting" ${BUILD_COMPLETE_NOTIFY}
        fi
    fi
    RELEASE_FILE_NAME=${INSTALL_FROM_RELEASE}.tar.gz
    cd ${SERVER_HOME}
    rm -f ${SERVER_HOME}/${INSTALL_FROM_RELEASE}.tar.gz*
    rm -rf *.tar.gz
    if [[ ${INSTALL_FROM_SCP_USER_AT_HOST_AND_PORT} != "" ]]; then
            SCP_PORT=$( echo ${INSTALL_FROM_SCP_USER_AT_HOST_AND_PORT} | sed -e 's/^[^:]*:\?\([0-9]*\)\?$/\1/' )
        if [ -n "${SCP_PORT}" ]; then
            SCP_PORT_OPTION="-P ${SCP_PORT}"
        fi
        SCP_HOST=$( echo ${INSTALL_FROM_SCP_USER_AT_HOST_AND_PORT} | sed -e 's/^\([^:]*\):\?\([0-9]*\)\?$/\1/' )
        scp ${SCP_PORT_OPTION} ${SCP_HOST}:/home/trac/releases/${INSTALL_FROM_RELEASE}/${RELEASE_FILE_NAME} .
    else
        echo "Loading from release file https://releases.sapsailing.com/${INSTALL_FROM_RELEASE}/${RELEASE_FILE_NAME}"
        wget https://releases.sapsailing.com/${INSTALL_FROM_RELEASE}/${RELEASE_FILE_NAME}
    fi
    load_from_local_release_file
}

load_from_local_release_file ()
{
    if [[ ${INSTALL_FROM_RELEASE} != "" ]]; then
        cd ${SERVER_HOME}
        rm -rf plugins start stop status native-libraries org.eclipse.osgi
        echo "Loading from release file ${INSTALL_FROM_RELEASE}"
        mv env.sh env.sh.preserved
        mv configuration/mail.properties configuration/mail.properties.preserved
        mv configuration/debug.properties configuration/debug.properties.preserved
        tar xvzf ${INSTALL_FROM_RELEASE}.tar.gz
        mv env.sh.preserved env.sh
        mv configuration/mail.properties.preserved configuration/mail.properties
        mv configuration/debug.properties.preserved configuration/debug.properties
        # Try to create the symbolic links to the /home/scores directories where uploaded results are mounted through NFS if in the right region
        find /home/scores/* -type d -prune -exec ln -s {} \; 2>/dev/null >/dev/null
        echo "Configuration for this server is unchanged - just binaries have been changed."
    else
        echo "The variable INSTALL_FROM_RELEASE has not been set, therefore no release file will be installed!"
    fi
}

checkout_code ()
{
    cd $PROJECT_HOME
    GIT_BINARY=`which git`
    if [[ $COMPILE_GWT == "True" ]]; then
        # only reset if GWT gets compiled
        # if not p2build will not work
        $GIT_BINARY reset --hard
    fi
    $GIT_BINARY checkout $BUILD_FROM
    $GIT_BINARY pull
}

build ()
{
    # check for available memory - build can not be started with less than 1GB
    MEM_TOTAL=`free -mt | grep Total | awk '{print $2}'`
    if [ $MEM_TOTAL -lt 924 ]; then
        echo "Could not start build process with less than 1GB of RAM!"
        if which mail; then
            if [ -n "${BUILD_COMPLETE_NOTIFY}" ]; then
              echo "Not enough RAM for completing the build process! You need at least 1GB. Instance NOT started!" | mail -r noreply@sapsailing.com -s "Build of $INSTANCE_ID failed" ${BUILD_COMPLETE_NOTIFY}
            fi
        fi
    else
        if [[ $BUILD_BEFORE_START == "True" ]]; then
            cd $PROJECT_HOME
            TESTS="-t"
            if [[ $RUN_TESTS == "True" ]]; then
                TESTS=""
            fi
            GWT="-g"
            if [[ $COMPILE_GWT == "True" ]]; then
                GWT=""
            fi
            $PROJECT_HOME/configuration/buildAndUpdateProduct.sh $TESTS $GWT -u build
            STATUS=$?
            if [ $STATUS -eq 0 ]; then
                echo "Build Successful"
            else
                echo "Build Failed"
                exit 10
            fi 
        else
            echo "The parameter BUILD_BEFORE_START is not set to True therefore no build will be executed!"
        fi
    fi
}

deploy ()
{
    cd $PROJECT_HOME
    if [[ $DEPLOY_TO != "" ]]; then
        DEPLOY="-s $DEPLOY_TO"
    fi

    $PROJECT_HOME/configuration/buildAndUpdateProduct.sh -u $DEPLOY install > $SERVER_HOME/last_automatic_build.txt
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Deployment Successful"
        if which mail; then
            if [ -n "${BUILD_COMPLETE_NOTIFY}" ]; then
              echo "OK - check the attachment for more information." | mail -r noreply@sapsailing.com $MAIL_ATTACH_OPTION $SERVER_HOME/last_automatic_build.txt -s "Build or Deployment of $INSTANCE_ID complete" ${BUILD_COMPLETE_NOTIFY}
            fi
        fi
    else
        echo "Deployment Failed"
        if which mail; then
            if [ -n "${BUILD_COMPLETE_NOTIFY}" ]; then
              echo "ERROR - check the attachment for more information." | mail -r noreply@sapsailing.com $MAIL_ATTACH_OPTION $SERVER_HOME/last_automatic_build.txt -s "Build of $INSTANCE_ID failed" ${BUILD_COMPLETE_NOTIFY}
            fi
        fi
    fi 
}

auto_install ()
{
        # activate everything found in user data
        activate_user_data
        # Now build or fetch the correct release, based on activated user data:
        if [[ $BUILD_BEFORE_START = "True" ]]; then
            checkout_code
            build
            deploy
        else
            load_from_release_file
        fi
        # then download and install environment and append to env.sh
        install_environment
        # then append user data to env.sh as it shall take precedence over the installed environment's defaults
        append_user_data_to_envsh
        # then append the rules that compute defaults for variables not set elsewhere; this has to come last:
        append_default_envsh_rules
        # make sure to reload data, this time including defaults from release's env.sh, environment settings and user data
        source `pwd`/env.sh
        . generateMailProperties.sh
        echo ""
        echo "INSTALL_FROM_RELEASE: $INSTALL_FROM_RELEASE"
        echo "DEPLOY_TO: $DEPLOY_TO"
        echo "BUILD_BEFORE_START: $BUILD_BEFORE_START"
        echo "USE_ENVIRONMENT: $USE_ENVIRONMENT"
        echo ""
}

OPERATION=$1
PARAM=$2

checks
if [[ $OPERATION == "auto-install" ]]; then
    if [[ ! -z "$ON_AMAZON" ]]; then
        # first check and activate everything found in user data
        copy_user_data_to_tmp_file
        auto_install
    else
        echo "This server does not seem to be running on Amazon! Automatic install only works on Amazon instances."
        exit 1
    fi
    rm "${ec2EnvVars_tmpFile}"

elif [[ $OPERATION == "auto-install-from-stdin" ]]; then
    # copy stdin to user data tmp file,
    cat >"${ec2EnvVars_tmpFile}"
    # then auto-install
    auto_install
    rm "${ec2EnvVars_tmpFile}"

elif [[ $OPERATION == "install-release" ]]; then
    INSTALL_FROM_RELEASE=$PARAM
    # Honor the no-overrite setting if there is one
    if [ -f $SERVER_HOME/no-overwrite ]; then
        echo "Found a no-overwrite file in the servers directory. Please remove it to complete this operation!"
    else
        load_from_release_file
        echo "ATTENTION: This new release code is not active yet. Make sure to restart the server if it is running!"
    fi

elif [[ $OPERATION == "install-local-release" ]]; then
    INSTALL_FROM_RELEASE=$PARAM
    if [[ $INSTALL_FROM_RELEASE == "" ]]; then
        echo "You need to provide the file of a tar.gz release file, without the .tar.gz suffix"
        exit 1
    fi

    # Honor the no-overrite setting if there is one
    if [ -f $SERVER_HOME/no-overwrite ]; then
        echo "Found a no-overwrite file in the servers directory. Please remove it to complete this operation!"
    else
        load_from_local_release_file
        echo "ATTENTION: This new release code is not active yet. Make sure to restart the server if it is running!"
    fi

elif [[ $OPERATION == "install-env" ]]; then
    USE_ENVIRONMENT=$PARAM
    if [[ $USE_ENVIRONMENT == "" ]]; then
        echo "You need to provide the name of an environment from https://releases.sapsailing.com/environments"
        exit 1
    fi

    if [ -f $SERVER_HOME/no-overwrite ]; then
        echo "Found a no-overwrite file in the servers directory. Please remove it to complete this operation!"
    else
        install_environment
        # make sure to reload data
        source `pwd`/env.sh

        echo "Configuration for this server is now:"
        echo ""
        echo "SERVER_NAME: $SERVER_NAME"
        echo "MEMORY: $MEMORY"
        echo "SERVER_PORT: $SERVER_PORT"
        echo "TELNET_PORT: $TELNET_PORT"
        echo "MONGODB_HOST: $MONGODB_HOST"
        echo "MONGODB_PORT: $MONGODB_PORT"
        echo "MONGODB_URI: $MONGODB_URI"
        echo "EXPEDITION_PORT: $EXPEDITION_PORT"
        echo "REPLICATION_HOST: $REPLICATION_HOST"
        echo "REPLICATION_CHANNEL: $REPLICATION_CHANNEL"
        echo "ADDITIONAL_ARGS: $ADDITIONAL_JAVA_ARGS"
        echo ""
        echo "INSTALL_FROM_RELEASE: $INSTALL_FROM_RELEASE"
        echo "DEPLOY_TO: $DEPLOY_TO"
        echo "BUILD_BEFORE_START: $BUILD_BEFORE_START"
        echo "USE_ENVRIONMENT: $USE_ENVIRONMENT"
        echo ""
        echo "JAVA_HOME: $JAVA_HOME"
        echo "INSTANCE_ID: $INSTANCE_ID"
        echo ""
        echo "ATTENTION: This new configuration is not active yet. Make sure to restart the server if it is running!"
    fi
elif [[ $OPERATION == "install-user-data" ]]; then
    if [ -f $SERVER_HOME/no-overwrite ]; then
        echo "Found a no-overwrite file in the servers directory. Please remove it to complete this operation!"
    else
        append_user_data_to_envsh
    fi
else
    echo "Script to prepare a Java instance running on Amazon."
    echo ""
    echo "auto-install: downloads, builds, or takes from a local file a release, unpacks it in the current directory and applies the environment, user data, and environment defaults to the env.sh file"
    echo "auto-install-from-stdin: like auto-install, only that the additional configuration data is sourced from standard input (stdin), not the AWS EC2 instance's user data."
    echo "install-release <release>: Downloads the release specified by the second option and overwrites all code for this server. Preserves env.sh."
    echo "install-local-release <release-file>: Installs the release file specified by the second option and overwrites all code for this server. Preserves env.sh."
    echo "install-env <environment>: Downloads and updates the environment with the one specified as a second option. Does NOT take into account Amazon user-data!"
    echo "install-user-data: appends the user data set for the EC2 instance to the env.sh file"
    exit 0
fi
