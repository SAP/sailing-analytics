#!/bin/sh

source `pwd`/env.sh

DATE_OF_EXECUTION=`date`

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
        JAVA_VERSION=$(java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
        if [ "$JAVA_VERSION" -lt 17 ]; then
            echo "The current Java version ($JAVA_VERSION) does not match the requirements (>= 1.7)."
            exit 10
        fi
        JAVA_BINARY=`which java`
        echo "Using Java from $JAVA_BINARY"
    fi

    # make sure to set email adresses
    if [[ $BUILD_COMPLETE_NOTIFY == "" ]]; then
        export BUILD_COMPLETE_NOTIFY=simon.marcel.pamies@sap.com
    fi

    if [[ $SERVER_STARTUP_NOTIFY == "" ]]; then
        export SERVER_STARTUP_NOTIFY=simon.marcel.pamies@sap.com
    fi

    if [[ $DEPLOY_TO == "" ]]; then
        DEPLOY_TO=server
    fi
}

activate_user_data ()
{
    echo "Reading user-data provided by Amazon instance data to $USER_HOME/servers/$DEPLOY_TO/env.sh"

    # make backup of original file
    cp $USER_HOME/servers/$DEPLOY_TO/env.sh $USER_HOME/servers/$DEPLOY_TO/environment/env.sh.backup

    echo "# User-Data: START ($DATE_OF_EXECUTION)" >> $USER_HOME/servers/$DEPLOY_TO/env.sh
    echo "INSTANCE_NAME=`ec2-metadata -i | cut -f2 -d \" \"`" >> $USER_HOME/servers/$DEPLOY_TO/env.sh
    echo "INSTANCE_IP4=`ec2-metadata -v | cut -f2 -d \" \"`" >> $USER_HOME/servers/$DEPLOY_TO/env.sh
    echo "INSTANCE_DNS=`ec2-metadata -p | cut -f2 -d \" \"`" >> $USER_HOME/servers/$DEPLOY_TO/env.sh

    VARS=$(ec2-metadata -d | sed "s/user-data\: //g")
    for var in $VARS; do
        echo $var >> $USER_HOME/servers/$DEPLOY_TO/env.sh
        echo "Activated: $var"
    done
    echo "# User-Data: END" >> $USER_HOME/servers/$DEPLOY_TO/env.sh
    
    # make sure to reload data
    source `pwd`/env.sh
    if [[ $DEPLOY_TO == "" ]]; then
        DEPLOY_TO=server
        echo "DEPLOY_TO=server" >> $USER_HOME/servers/server/env.sh
    fi

    echo "INSTANCE_ID=\"$INSTANCE_NAME ($INSTANCE_IP4)\"" >> $USER_HOME/servers/$DEPLOY_TO/env.sh
    echo "Updated env.sh with data from user-data field!"
    echo ""
}

install_environment ()
{
    if [[ $USE_ENVIRONMENT != "" ]]; then
        # clean up directory to really make sure that there are no files left
        rm -rf $USER_HOME/servers/$DEPLOY_TO/environment
        mkdir $USER_HOME/servers/$DEPLOY_TO/environment
        echo "Using environment http://releases.sapsailing.com/environments/$USE_ENVIRONMENT"
        wget -P environment http://releases.sapsailing.com/environments/$USE_ENVIRONMENT
        echo "# Environment: START ($DATE_OF_EXECUTION)" >> $USER_HOME/servers/$DEPLOY_TO/env.sh
        cat $USER_HOME/servers/server/environment/$USE_ENVIRONMENT >> $USER_HOME/servers/$DEPLOY_TO/env.sh
        echo "# Environment: END" >> $USER_HOME/servers/$DEPLOY_TO/env.sh

        # make sure to reload data
        source `pwd`/env.sh
        if [[ $DEPLOY_TO == "" ]]; then
            DEPLOY_TO=server
            echo "DEPLOY_TO=server" >> $USER_HOME/servers/server/env.sh
        fi

        echo "Updated env.sh with data from environment file!"
    else
        echo "No environment file specified!"
    fi
}

load_from_release_file ()
{
    if [[ $INSTALL_FROM_RELEASE != "" ]]; then
        echo "Build/Deployment process has been started - it can take 5 to 20 minutes until your instance is ready. " | mail -r simon.marcel.pamies@sap.com -s "Build or Deployment of $INSTANCE_ID starting" $BUILD_COMPLETE_NOTIFY
        cd $USER_HOME/servers/$DEPLOY_TO
        rm -f $USER_HOME/servers/server/$INSTALL_FROM_RELEASE.tar.gz*
        rm -rf plugins start stop status native-libraries org.eclipse.osgi *.tar.gz
        echo "Loading from release file http://releases.sapsailing.com/$INSTALL_FROM_RELEASE/$INSTALL_FROM_RELEASE.tar.gz"
        wget http://releases.sapsailing.com/$INSTALL_FROM_RELEASE/$INSTALL_FROM_RELEASE.tar.gz
        mv env.sh env.sh.preserved
        tar xvzf $INSTALL_FROM_RELEASE.tar.gz
        mv env.sh.preserved env.sh
        echo "Configuration for this server is unchanged - just binaries have been changed."
    else
        echo "The variable INSTALL_FROM_RELEASE has not been set therefore no release file will be installed!"
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
        echo "Not enough RAM for completing the build process! You need at least 1GB. Instance NOT started!" | mail -r simon.marcel.pamies@sap.com -s "Build of $INSTANCE_ID failed" $BUILD_COMPLETE_NOTIFY
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

    $PROJECT_HOME/configuration/buildAndUpdateProduct.sh -u $DEPLOY install > $USER_HOME/servers/$DEPLOY_TO/last_automatic_build.txt
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Deployment Successful"
        echo "OK - check the attachment for more information." | mail -a $USER_HOME/servers/$DEPLOY_TO/last_automatic_build.txt -r simon.marcel.pamies@sap.com -s "Build or Deployment of $INSTANCE_ID complete" $BUILD_COMPLETE_NOTIFY
    else
        echo "Deployment Failed"
        echo "ERROR - check the attachment for more information." | mail -a $USER_HOME/servers/$DEPLOY_TO/last_automatic_build.txt -r simon.marcel.pamies@sap.com -s "Build of $INSTANCE_ID failed" $BUILD_COMPLETE_NOTIFY
    fi 
}

OPERATION=$1
PARAM=$2

checks
if [[ $OPERATION == "auto-install" ]]; then
    if [[ ! -z "$ON_AMAZON" ]]; then
        # first check and activate everything found in user data
        # then download and install environment
        activate_user_data
        install_environment

        if [[ $INSTALL_FROM_RELEASE == "" ]] && [[ $BUILD_BEFORE_START != "True" ]]; then
            echo "It could not find any option telling me to download a release or to build! Possible cause: Your environment contains empty values for these variables!"
            exit 1
        fi

        echo ""
        echo "INSTALL_FROM_RELEASE: $INSTALL_FROM_RELEASE"
        echo "DEPLOY_TO: $DEPLOY_TO"
        echo "BUILD_BEFORE_START: $BUILD_BEFORE_START"
        echo "USE_ENVRIONMENT: $USE_ENVIRONMENT"
        echo ""

        if [[ $INSTALL_FROM_RELEASE != "" ]]; then
            load_from_release_file
        else
            if [[ $BUILD_BEFORE_START == "True" ]]; then
                checkout_code
                build
                deploy
            fi
        fi
    else
        echo "This server does not seem to be running on Amazon! Automatic install only works on Amazon instances."
        exit 1
    fi

elif [[ $OPERATION == "install-release" ]]; then
    INSTALL_FROM_RELEASE=$PARAM
    if [[ $INSTALL_FROM_RELEASE == "" ]]; then
        echo "You need to provide the name of a release from http://releases.sapsailing.com/"
        exit 1
    fi

    # Honor the no-overrite setting if there is one
    if [ ! -f $USER_HOME/servers/$DEPLOY_TO/no-overwrite ]; then
        echo "Found a no-overwrite file in the servers directory. Please remove it to complete this operation!"
    else
        load_from_release_file
    fi

elif [[ $OPERATION == "update-environment" ]]; then
    USE_ENVIRONMENT=$PARAM
    if [[ $USE_ENVIRONMENT == "" ]]; then
        echo "You need to provide the name of an environment from http://releases.sapsailing.com/environments"
        exit 1
    fi

    if [ ! -f $USER_HOME/servers/$DEPLOY_TO/no-overwrite ]; then
        echo "Found a no-overwrite file in the servers directory. Please remove it to complete this operation!"
    else
        install_environment
        echo "Configuration for this server is now:"
        echo ""
        echo "SERVER_NAME: $SERVER_NAME"
        echo "MEMORY: $MEMORY"
        echo "SERVER_PORT: $SERVER_PORT"
        echo "TELNET_PORT: $TELNET_PORT"
        echo "MONGODB_HOST: $MONGODB_HOST"
        echo "MONGODB_PORT: $MONGODB_PORT"
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
    fi
else
    echo "Script to prepare a Java instance running on Amazon."
    echo ""
    echo "install-release <release>: Downloads the release specified by the second option and overwrites all code for this server. Preserves env.sh."
    echo "update-env <environment>: Downloads and updates the environment with the one specified as a second option."
    exit 0
fi

