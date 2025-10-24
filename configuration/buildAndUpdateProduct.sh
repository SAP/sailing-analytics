#!/bin/bash
set -o functrace
source ./configuration/correctFilePathInRelationToCurrentOs.sh

# This indicates the type of the project
# and is used to correctly resolve bundle names
PROJECT_TYPE="sailing"

find_project_home ()
{
    if [[ "$1" == '/' ]] || [[ "$1" == "" ]]; then
        echo ""
        return 0
    fi

    if [ ! -d "$1/.git" ]; then
        PARENT_DIR="`cd "$1/..";pwd`"
        OUTPUT=$(find_project_home "$PARENT_DIR")

        if [ "$OUTPUT" = "" ] && [ -d "$PARENT_DIR/$CODE_DIRECTORY" ] && [ -d "$PARENT_DIR/$CODE_DIRECTORY/.git" ]; then
            OUTPUT="$PARENT_DIR/$CODE_DIRECTORY"
        fi
        echo $OUTPUT
        return 0
    fi

    echo $(correct_file_path  "$1")
}

clean_gwt_artifacts ()
{
    cd $PROJECT_HOME/java
    rm -rf com.sap.$PROJECT_TYPE.gwt.ui/com.sap.$PROJECT_TYPE.* com.sap.$PROJECT_TYPE.gwt.ui/.generated
    rm -rf com.sap.sailing.dashboards.gwt/com.sap.sailing.dashboards.gwt.* com.sap.sailing.dashboards.gwt/.generated
    rm -rf com.sap.sse.security.ui/com.sap.sse.security.ui.* com.sap.sse.security.ui/.generated
    rm -rf com.sap.sse.gwt/com.sap.sse.gwt.* com.sap.sse.gwt/.generated
}

set_ACDIR_and_depending_variables () {
    ACDIR="$1"
    JETTY_CONFIG_DIR="${ACDIR}/${JETTY_CONFIG_SUBDIR}"
    VERSION_TXT="${JETTY_CONFIG_DIR}/version.txt"
    VERSION_JSON="${JETTY_CONFIG_DIR}/version.json"
}

create_version_info_files ()
{
    echo "$VERSION_INFO System:" > "${VERSION_TXT}"
    echo "{
    \"commit_id\":\"$HEAD_SHA\",
    \"active_branch\":\"$active_branch\",
    \"build_date\":\"$HEAD_DATE\",
    \"release\":\"$SIMPLE_VERSION_INFO\"
}" > "${VERSION_JSON}"
}

# this holds for default installation
USER_HOME=~
START_DIR="`pwd`"

if [ "$PROJECT_HOME" = "" ]; then
    PROJECT_HOME=$(find_project_home "$START_DIR")
fi

# if project_home is still empty we could not determine any suitable directory
if [[ "$PROJECT_HOME" == "" ]]; then
    echo "Could neither determine nor get PROJECT_HOME. Please provide it by setting an environment variable with this name."
    exit 1
fi

#reading the filepath and editing it, so it fits for eclipse #currently save works for cygwin, gitbash and linux
if [ "$SERVERS_HOME" = "" ]; then
    SERVERS_HOME=$(correct_file_path  "$USER_HOME/servers")
fi

# x86 or x86_64 should work for most cases
ARCH=x86_64
START_DIR=`pwd`

# needed for maven on sapsailing.com to work correctly
if [ -f "$USER_HOME/.bash_profile" ]; then
    source "$USER_HOME/.bash_profile"
fi

cd "$PROJECT_HOME"
active_branch=$(git symbolic-ref -q HEAD)
if [[ $active_branch == "" ]]; then
    active_branch="build"
else
    active_branch=`basename $active_branch`
fi

HEAD_SHA=$(git show-ref --head -s | head -1)
HEAD_DATE=$(date "+%Y%m%d%H%M")
VERSION_INFO="$HEAD_SHA-$active_branch-$HEAD_DATE"
SIMPLE_VERSION_INFO="$active_branch-$HEAD_DATE"
# The number of worker threads to use for building GWT permutations.
# Can be overridden using the -x option
GWT_WORKERS=2
# The default resolution for headless Firefox for Selenium tests:
export MOZ_HEADLESS_WIDTH=1600
export MOZ_HEADLESS_HEIGHT=900

MAVEN_SETTINGS="$PROJECT_HOME/configuration/maven-settings.xml"
MAVEN_SETTINGS_PROXY="$PROJECT_HOME/configuration/maven-settings-proxy.xml"

p2PluginRepository=$PROJECT_HOME/java/com.sap.$PROJECT_TYPE.feature.p2build/target/products/raceanalysis.product.id/linux/gtk/$ARCH

HAS_OVERWRITTEN_TARGET=0
TARGET_SERVER_NAME=$active_branch

gwtcompile=1
onegwtpermutationonly=0
testing=1
clean="clean"
offline=0
proxy=0
android=1
java=1
reporting=0
suppress_confirmation=0
export extra='--batch-mode -DtestSuffix=.noAutomaticTestingBasedOnBundleName'
parallelexecution=0
p2local=0

if [ $# -eq 0 ]; then
    echo "buildAndUpdateProduct [-b -u -g -t -a -r -o -c -p -v -m <config> -n <package> -l <port> -x <gwt-workers> -j <test-package>] [build|install|all|hot-deploy|remote-deploy|local-deploy|release]"
    echo ""
    echo "-g Disable GWT compile, no gwt files will be generated, old ones will be preserved."
    echo "-G Build forked GWT and gwt-maven-plugin locally instead of downloading Github release."
    echo "-b Build GWT permutation only for one browser and English language."
    echo "-t Disable tests"
    echo "-a Disable mobile projects (RaceCommittee App, e.g., in case no AndroidSDK is installed)"
    echo "-A Only build mobile projects (e.g. RaceCommittee App) and skip backend/server build"
    echo "-r Enable generating surefire test reports"
    echo "-o Enable offline mode (does not work for tycho surefire plugin)"
    echo "-c Disable cleaning (use only if you are sure that no java file has changed)"
    echo "-p Enable proxy mode (overwrites file specified by -m)"
    echo "-m <path to file> Specify alternate maven configuration (possibly has side effect on proxy setting)"
    echo "-n <package name> Name of the bundle you want to hot deploy. Needs fully qualified name like"
    echo "                  com.sap.sailing.monitoring. Only works if there is a fully built server available."
    echo "                  This parameter can also hold the name of the release if you are using the release command."
    echo "-l <telnet port>  Telnet port the OSGi server is running. Optional but enables fully automatic hot-deploy."
    echo "-L in conjunction with the release sub-command, build the release only locally to dist/ and do not upload"
    echo "-s <target server> Name of server you want to use as target for install, hot-deploy or remote-reploy. This overrides default behaviour."
    echo "-w <ssh target> Target for remote-deploy and release. Must comply with the following format: user@server."
    echo "-u Run without confirmation messages. Use with extreme care."
    echo "-v Build local p2 respository, and use this instead of p2.sapsailing.com"
    echo "-x <number-of-workers> use this many worker threads for building GWT permutations (default: 2)."
    echo "-j <test-package> only execute the provided test package during tests"
    echo ""
    echo "build: builds the server code using Maven to $PROJECT_HOME (log to $START_DIR/build.log)"
    echo ""
    echo "install: installs product files to $SERVERS_HOME/$active_branch. Does NOT overwrite any configuration in env.sh! If you want to"
    echo "         overwrite the configuration then use the refreshInstance.sh script that comes with the instance. "
    echo ""
    echo "all: invokes build and then install"
    echo ""
    echo "release: Releases a server package to the location specified by -w parameter. The release is named using the branch name and the date."
    echo "You can overwrite the generated release name by specifying a name with the parameter -n. Do not use spaces or other special characters!"
    echo "Example: $0 -w trac@sapsailing.com -n release-ess-brazil-2013 release"
    echo ""
    echo "hot-deploy: performs hot deployment of named bundle into OSGi server."
    echo "Example: $0 -n com.sap.sailing.www -l 14888 hot-deploy"
    echo ""
    echo "local-deploy: performs deployment of one or more bundles into a local directory"
    echo "Example: $0 -n com.sap.sailing.www -s /home/user/myserver local-deploy"
    echo ""
    echo "remote-deploy: deploys the last build results to a remote server and optionally re-starts it"
    echo "Example: $0 -s dev -w trac@sapsailing.com remote-deploy"
    echo ""
    echo "clean: cleans all code and GWT files"
    echo ""
    echo "Active branch is $active_branch"
    echo "Project home is $PROJECT_HOME"
    echo "Server home is $SERVERS_HOME"
    echo "Version info: $VERSION_INFO"
    echo "P2 home is $p2PluginRepository"
    exit 2
fi

echo PROJECT_HOME is $PROJECT_HOME
echo SERVERS_HOME is $SERVERS_HOME
echo BRANCH is $active_branch
echo VERSION is $VERSION_INFO

options=':bgtocpaArvmLG:n:l:s:w:x:j:u'
while getopts $options option
do
    case $option in
        g) gwtcompile=0;;
        G) BUILD_GWT_FORK=1;;
        t) testing=0;;
        b) onegwtpermutationonly=1;;
        o) offline=1;;
        c) clean="";;
        p) proxy=1;;
        a) android=0;;
        A) android=1
           java=0;;
        r) reporting=1;;
        m) MAVEN_SETTINGS=$OPTARG;;
        n) OSGI_BUNDLE_NAME=$OPTARG;;
        l) OSGI_TELNET_PORT=$OPTARG;;
        L) LOCAL_RELEASE_ONLY=1;;
        s) TARGET_SERVER_NAME=$OPTARG
           HAS_OVERWRITTEN_TARGET=1;;
        w) REMOTE_SERVER_LOGIN=$OPTARG;;
        u) suppress_confirmation=1;;
        v) p2local=1;;
        x) GWT_WORKERS=$OPTARG;;
        j) TESTCASE_TO_EXECUTE=$OPTARG;;
        \?) echo "Invalid option"
            exit 4;;
    esac
done

JETTY_CONFIG_SUBDIR=configuration/jetty
set_ACDIR_and_depending_variables "${SERVERS_HOME}/${TARGET_SERVER_NAME}"
echo INSTALL goes to $ACDIR
echo TMP will be used for java.io.tmpdir and is $TMP
if [ "$TMP" = "" ]; then
  export TMP=/tmp
fi
extra="${extra} -Dgwt.workers=${GWT_WORKERS} -Djava.io.tmpdir=$TMP -Dgwt.workDir=$TMP"
extra="${extra} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0"

shift $((OPTIND-1))

if [[ $@ == "" ]]; then
	echo "You need to specify an action [build|install|all|hot-deploy|remote-deploy|release]"
	exit 2
fi

rm $START_DIR/build.log

if [[ "$@" == "clean" ]]; then
    JAVA_HOME="${JAVA8_HOME}" ./gradlew clean
    if [[ $? != 0 ]]; then
        exit 100
    fi
    clean_gwt_artifacts
    cd $PROJECT_HOME
    echo "Using following command: mvn $extra -DargLine=\"$APP_PARAMETERS\" -fae -s $MAVEN_SETTINGS $clean"
    echo "Maven version used: `mvn --version`"
    mvn $extra -DargLine="$APP_PARAMETERS" -fae -s $MAVEN_SETTINGS $clean 2>&1 | tee -a $START_DIR/build.log
    MVN_EXIT_CODE=${PIPESTATUS[0]}
    echo "Maven exit code is $MVN_EXIT_CODE"
    exit 0
fi

if [[ "$@" == "release" ]]; then
    if [ ! -d $p2PluginRepository/plugins ]; then
        echo "Could not find source directory $p2PluginRepository!"
        exit 1
    fi

    RELEASE_NOTES=""
    COMMIT_WEEK_COUNT=4
    if [ $suppress_confirmation -eq 0 ]; then
        echo ""
        echo "Please provide me with some notes about this release. You can add more than"
        echo "one line. Please include major changes or new features. After your notes I will"
        echo "also include the commits of the last 4 weeks. You can save and quit by hitting ctrl+d."
        while read -e -p "> " line; do
            RELEASE_NOTES="$RELEASE_NOTES\n$line"
        done

        if [[ $RELEASE_NOTES == "" ]]; then
            echo -e "\nCome on - I can not release without at least some notes about this release!"
            exit 1
        fi
        echo -e "\nThank you! One last thing..."

        echo "How many weeks of commits do you want to include (0=No commits)?"
        read -p "> " -e COMMIT_WEEK_COUNT
    fi

    mkdir -p $PROJECT_HOME/dist
    rm -rf $PROJECT_HOME/dist/*
    mkdir -p $PROJECT_HOME/build

    RELEASE_NOTES="Release ${VERSION_INFO}\n${RELEASE_NOTES}"
    echo -e $RELEASE_NOTES > $PROJECT_HOME/build/release-notes.txt
    echo "" >> $PROJECT_HOME/build/release-notes.txt
    echo "Commits for the last $COMMIT_WEEK_COUNT weeks:" >> $PROJECT_HOME/build/release-notes.txt
    echo "" >> $PROJECT_HOME/build/release-notes.txt
    cd $PROJECT_HOME
    git log --decorate --pretty=format:"%h - %an, %ar : %s" --date=relative --abbrev-commit --since=$COMMIT_WEEK_COUNT.weeks >> $PROJECT_HOME/build/release-notes.txt

    set_ACDIR_and_depending_variables "${PROJECT_HOME}/build"
    cd "${ACDIR}"

    mkdir -p ${JETTY_CONFIG_SUBDIR}/etc
    mkdir plugins

    cp -v $PROJECT_HOME/java/target/start $ACDIR/
    cp -v $PROJECT_HOME/java/target/stop $ACDIR/
    cp -v $PROJECT_HOME/java/target/status $ACDIR/
    cp -v $PROJECT_HOME/java/target/configuration/JavaSE-11.profile $ACDIR/
    cp -v $PROJECT_HOME/java/target/refreshInstance.sh $ACDIR/
    cp -v $PROJECT_HOME/java/target/stopReplicating.sh $ACDIR/
    cp -v $PROJECT_HOME/java/target/generateMailProperties.sh $ACDIR/

    cp -v $PROJECT_HOME/java/target/env.sh $ACDIR/
    cp -v $PROJECT_HOME/java/target/env-default-rules.sh $ACDIR/
    cp -v $PROJECT_HOME/java/target/defineReverseProxyMappings.sh $ACDIR/
    cp -v $p2PluginRepository/configuration/config.ini configuration/

    cp -v $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty.xml ${JETTY_CONFIG_SUBDIR}/etc
    cp -v $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty-http.xml ${JETTY_CONFIG_SUBDIR}/etc
    cp -v $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty-deploy.xml ${JETTY_CONFIG_SUBDIR}/etc
    cp -v $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/realm.properties ${JETTY_CONFIG_SUBDIR}/etc
    cp -v $PROJECT_HOME/java/target/configuration/monitoring.properties configuration/
    cp -v $PROJECT_HOME/java/target/configuration/mail.properties configuration/
    cp -v $PROJECT_HOME/java/target/configuration/debug.properties configuration/
    cp -v $PROJECT_HOME/configuration/mongodb.cfg $ACDIR/
    cp -v $PROJECT_HOME/java/target/udpmirror $ACDIR/
    cp -v $PROJECT_HOME/java/target/http2udpmirror $ACDIR
    cp -v $PROJECT_HOME/java/target/configuration/logging.properties $ACDIR/configuration
    cp -r -v $p2PluginRepository/configuration/org.eclipse.equinox.simpleconfigurator configuration/
    cp -vr $p2PluginRepository/plugins $ACDIR/
    cp -rv $PROJECT_HOME/configuration/native-libraries $ACDIR/
    cp -v $PROJECT_HOME/configuration/buildAndUpdateProduct.sh $ACDIR/

    if [[ $OSGI_BUNDLE_NAME != "" ]]; then
        SIMPLE_VERSION_INFO="$OSGI_BUNDLE_NAME-$HEAD_DATE"
    fi
    create_version_info_files

    # removing compile reports as they do not belong into a release
    find $ACDIR -name soycReport | xargs rm -rf

    mkdir $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO
    echo "MONGODB_NAME=myspecificevent
REPLICATION_CHANNEL=myspecificevent
SERVER_NAME=MYSPECIFICEVENT
USE_ENVIRONMENT=live-server
MEMORY=4096m
INSTALL_FROM_RELEASE=$SIMPLE_VERSION_INFO
    " >> $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO/amazon-launch-config.txt

    echo "MONGODB_NAME=myspecificevent
REPLICATION_CHANNEL=name_of_replication_channel_matching_master_config
REPLICATE_MASTER_EXCHANGE_NAME=myspecificevent_name_used_as_channel
REPLICATE_MASTER_SERVLET_HOST=ip_of_master_host
SERVER_NAME=subdomain_name_in_elb
EVENT_ID=event_uid_this_replica_is_serving
USE_ENVIRONMENT=replica
MEMORY=4096m
REPLICATE_ON_START=com.sap.sailing.server.impl.RacingEventServiceImpl
INSTALL_FROM_RELEASE=$SIMPLE_VERSION_INFO
    " >> $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO/amazon-launch-config_replica.txt

    `which tar` cvzf $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO/$SIMPLE_VERSION_INFO.tar.gz *
    cp $ACDIR/env.sh $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO
    cp $PROJECT_HOME/build/release-notes.txt $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO

    cd $PROJECT_HOME
    rm -rf build/*

    SSH_CMD="ssh $REMOTE_SERVER_LOGIN"
    SCP_CMD="scp -r"

    echo "Packaged release $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO.tar.gz! I've put an env.sh that matches the current branch to $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO/env.sh!"

    if [ "${LOCAL_RELEASE_ONLY}" != "1" ]; then
        echo "Checking the remote connection..."
        REMOTE_HOME=`ssh $REMOTE_SERVER_LOGIN 'echo $HOME/releases'`
        echo "Now uploading release to $REMOTE_SERVER_LOGIN:$REMOTE_HOME. Can take quite a while!"

        `which scp` -r $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO $REMOTE_SERVER_LOGIN:$REMOTE_HOME/
        echo "Uploaded release to $REMOTE_HOME! Make sure to also put an updated env.sh if needed to the right place ($REMOTE_HOME/environment in most cases)"
    else
        echo "Release available at $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO/, tarball at $PROJECT_HOME/dist/$SIMPLE_VERSION_INFO/$SIMPLE_VERSION_INFO.tar.gz"
    fi
fi

if [[ "$@" == "local-deploy" ]]; then
    # check parameters
    if [[ $OSGI_BUNDLE_NAME == "" ]]; then
        echo "You need to provide -n parameter with bundle name."
        exit 1
    fi

    if [ ! -d $p2PluginRepository/plugins ]; then
        echo "Could not find source directory $p2PluginRepository!"
        exit 1
    fi

    if [[ $HAS_OVERWRITTEN_TARGET -eq 1 ]]; then
        TARGET_DIR=$TARGET_SERVER_NAME
    else
        echo "Please specify a local directory where the server resides using -s parameter"
    fi

    if [ ! -d $TARGET_DIR/plugins ]; then
        echo "Could not find target directory $TARGET_DIR/plugins!"
        exit 1
    fi

    # locate old bundle
    BUNDLE_COUNT=`find $TARGET_DIR/plugins -maxdepth 1 -name "${OSGI_BUNDLE_NAME}_*.jar" | wc -l`
    OLD_BUNDLE=`find $TARGET_DIR/plugins -maxdepth 1 -name "${OSGI_BUNDLE_NAME}_*.jar"`
    if [[ $OLD_BUNDLE == "" ]] || [[ $BUNDLE_COUNT -ne 1 ]]; then
        echo "ERROR: Could not find any bundle named $OSGI_BUNDLE_NAME ($BUNDLE_COUNT). Perhaps your name is misspelled or you have no build?"
        exit 1
    fi

    echo "Found $OLD_BUNDLE"
    OLD_BUNDLE_BASENAME=`basename $OLD_BUNDLE .jar`
    OLD_BUNDLE_VERSION=${OLD_BUNDLE_BASENAME#*_}

    echo "OLD bundle is $OSGI_BUNDLE_NAME with version $OLD_BUNDLE_VERSION"

    # locate new bundle
    NEW_BUNDLE=`find $p2PluginRepository/plugins -maxdepth 1 -name "${OSGI_BUNDLE_NAME}_*.jar"`
    NEW_BUNDLE_BASENAME=`basename $NEW_BUNDLE .jar`
    NEW_BUNDLE_VERSION=${NEW_BUNDLE_BASENAME#*_}
    echo "NEW bundle is $OSGI_BUNDLE_NAME with version $NEW_BUNDLE_VERSION"

    if [[ $NEW_BUNDLE_VERSION == $OLD_BUNDLE_VERSION ]]; then
        echo ""
        echo "WARNING: Bundle versions do not differ. Update not needed."
    fi

    if [ $suppress_confirmation -eq 0 ]; then
        read -s -n1 -p "Do you really want to locally deploy bundle $OSGI_BUNDLE_NAME to $TARGET_DIR? (y/N): " answer
        case $answer in
        "Y" | "y") echo "Continuing";;
        *) echo "Aborting..."
           exit 1;;
        esac
    fi

    # deploy new bundle physically
    echo ""Removing $OLD_BUNDLE...
    rm -f $OLD_BUNDLE
    cp $NEW_BUNDLE $TARGET_DIR/plugins
    echo "Copied ${NEW_BUNDLE_BASENAME}.jar to $TARGET_DIR/plugins. Please restart the server..."
    exit 0
fi


if [[ "$@" == "hot-deploy" ]]; then
    # check parameters
    if [[ $OSGI_BUNDLE_NAME == "" ]]; then
        echo "You need to provide -n parameter with bundle name."
        exit 1
    fi

    if [ ! -d $p2PluginRepository/plugins ]; then
        echo "Could not find source directory $p2PluginRepository!"
        exit 1
    fi

    if [[ $HAS_OVERWRITTEN_TARGET -eq 1 ]]; then
        active_branch=$TARGET_SERVER_NAME
    fi

    if [ ! -d $SERVERS_HOME/$active_branch/plugins ]; then
        echo "Could not find target directory $SERVERS_HOME/$active_branch/plugins!"
        exit 1
    fi

    # locate old bundle
    BUNDLE_COUNT=`find $SERVERS_HOME/$active_branch/plugins -maxdepth 1 -name "${OSGI_BUNDLE_NAME}_*.jar" | wc -l`
    OLD_BUNDLE=`find $SERVERS_HOME/$active_branch/plugins -maxdepth 1 -name "${OSGI_BUNDLE_NAME}_*.jar"`
    if [[ $OLD_BUNDLE == "" ]] || [[ $BUNDLE_COUNT -ne 1 ]]; then
        echo "ERROR: Could not find any bundle named $OSGI_BUNDLE_NAME ($BUNDLE_COUNT). Perhaps your name is misspelled or you have no build?"
        exit 1
    fi

    OLD_BUNDLE_BASENAME=`basename $OLD_BUNDLE .jar`
    OLD_BUNDLE_VERSION=${OLD_BUNDLE_BASENAME#*_}

    echo "OLD bundle is $OSGI_BUNDLE_NAME with version $OLD_BUNDLE_VERSION"

    # locate new bundle
    NEW_BUNDLE=`find $p2PluginRepository/plugins -maxdepth 1 -name "${OSGI_BUNDLE_NAME}_*.jar"`
    NEW_BUNDLE_BASENAME=`basename $NEW_BUNDLE .jar`
    NEW_BUNDLE_VERSION=${NEW_BUNDLE_BASENAME#*_}
    echo "NEW bundle is $OSGI_BUNDLE_NAME with version $NEW_BUNDLE_VERSION"

    if [[ $NEW_BUNDLE_VERSION == $OLD_BUNDLE_VERSION ]]; then
        echo ""
        echo "WARNING: Bundle versions do not differ. Update not needed."
    fi

    if [ $suppress_confirmation -eq 0 ]; then
        read -s -n1 -p "Do you really want to hot-deploy bundle $OSGI_BUNDLE_NAME to $SERVERS_HOME/$active_branch? (y/N): " answer
        case $answer in
        "Y" | "y") echo "Continuing";;
        *) echo "Aborting..."
           exit 1;;
        esac
    fi

    # deploy new bundle physically
    mkdir -p $SERVERS_HOME/$active_branch/plugins/deploy
    cp $NEW_BUNDLE $SERVERS_HOME/$active_branch/plugins/deploy
    echo "Copied ${NEW_BUNDLE_BASENAME}.jar to $SERVERS_HOME/$active_branch/plugins/deploy"

    # check telnet port connection
    TELNET_ACTIVE=`netstat -tlnp 2>/dev/null | grep ":$OSGI_TELNET_PORT"`
    if [[ $TELNET_ACTIVE == "" ]]; then
        # some BSD systems do not support -p
        TELNET_ACTIVE=`netstat -an | grep ".$OSGI_TELNET_PORT"`
    fi

    if [[ $OSGI_TELNET_PORT == "" ]] || [[ $TELNET_ACTIVE == "" ]]; then
        echo ""
        echo "ERROR: Could not find any process running on port $OSGI_TELNET_PORT. Make sure your server has been started with -console $OSGI_TELNET_PORT"
        echo "I've already deployed bundle to $SERVERS_HOME/$active_branch/plugins/deploy/${NEW_BUNDLE_BASENAME}.jar"
        echo "You can now install it yourself by issuing the following commands:"
        echo ""
        echo "osgi> ss $OSGI_BUNDLE_NAME"
        echo "21   ACTIVE   $OLD_BUNDLE_BASENAME"
        echo "osgi> stop 21"
        echo "osgi> uninstall 21"
        echo "osgi> install file://$SERVERS_HOME/$active_branch/plugins/deploy/${NEW_BUNDLE_BASENAME}.jar"
        echo "osgi> ss $OSGI_BUNDLE_NAME"
        echo "71   INSTALLED   $NEW_BUNDLE_BASENAME"
        echo "osgi> start 71"
        exit 1
    fi

    # first get bundle ID
    echo -n "Connecting to OSGi server..."
    NC_CMD="nc -t 127.0.0.1 $OSGI_TELNET_PORT"
    echo "OK"
    OLD_BUNDLE_INFORMATION=`echo ss | $NC_CMD | grep ${OSGI_BUNDLE_NAME}_`
    BUNDLE_ID=`echo $OLD_BUNDLE_INFORMATION | cut -d " " -f 1`
    OLD_ACTIVATED_NAME=`echo $OLD_BUNDLE_INFORMATION | cut -d " " -f 3`
    echo "Could identify bundle-id $BUNDLE_ID for $OLD_ACTIVATED_NAME"
	if [ $suppress_confirmation -eq 0 ]; then
        read -s -n1 -p "I will now stop and reinstall the bundle mentioned in the line above. Is this right? (y/N): " answer
        case $answer in
        "Y" | "y") echo "Continuing";;
        *) echo "Aborting..."
           exit 1;;
        esac
    fi

    # stop and uninstall
    echo stop $BUNDLE_ID | $NC_CMD > /dev/null
    echo uninstall $BUNDLE_ID | $NC_CMD > /dev/null

    # make sure bundle is removed
    UNINSTALL_INFORMATION=`echo ss | $NC_CMD | grep ${OSGI_BUNDLE_NAME}_`
    if [[ $UNINSTALL_INFORMATION == "" ]]; then
        echo "Uninstall procedure sucessful!"
    else
        echo "Something went wrong during uninstall. Please check error logs."
        exit 1
    fi

    # now reinstall bundle
    NEW_BUNDLE_ID=`echo install file://$SERVERS_HOME/$active_branch/plugins/deploy/${NEW_BUNDLE_BASENAME}.jar | $NC_CMD`
    NEW_BUNDLE_INFORMATION=`echo ss | $NC_CMD | grep ${OSGI_BUNDLE_NAME}_`
    NEW_BUNDLE_ID=`echo $NEW_BUNDLE_INFORMATION | cut -d " " -f 1`
    echo "Installed new bundle file://$SERVERS_HOME/$active_branch/plugins/deploy/${NEW_BUNDLE_BASENAME}.jar with id $NEW_BUNDLE_ID"

    # and start
    echo start $NEW_BUNDLE_ID | $NC_CMD > /dev/null && sleep 1
    NEW_BUNDLE_STATUS=`echo ss | $NC_CMD | grep ${OSGI_BUNDLE_NAME}_ | grep ACTIVE`
    if [[ $NEW_BUNDLE_STATUS == "" ]]; then
        echo "ERROR: Something went wrong with start of bundle. Please check if everything went ok."
        exit 1
    fi

    echo "Everything seems to be ok. Bundle hot-deployed to server with new id $NEW_BUNDLE_ID"
    exit 0
fi

echo "Starting $@ of server..."

if [[ "$@" == "build" ]] || [[ "$@" == "all" ]]; then
	# yield build so that we get updated product
        if [ $offline -eq 1 ]; then
            echo "INFO: Activating offline mode"
            extra="$extra -o"
        fi

        if [ $proxy -eq 1 ]; then
            echo "INFO: Activating proxy profile"
            extra="$extra -P no-debug.with-proxy"
            MAVEN_SETTINGS=$MAVEN_SETTINGS_PROXY
	    ANDROID_OPTIONS="--proxy_host=proxy --proxy_port=8080"
        else
            extra="$extra -P no-debug.without-proxy"
	    ANDROID_OPTIONS=""
        fi

	cd $PROJECT_HOME/java
	if [ $gwtcompile -eq 1 ] && [[ "$clean" == "clean" ]]; then
	    echo "INFO: Compiling GWT (rm -rf com.sap.$PROJECT_TYPE.gwt.ui/com.sap.$PROJECT_TYPE.*)"
	    clean_gwt_artifacts
	    GWT_XML_FILES=`find . -name '*.gwt.xml'`
	    if [ $onegwtpermutationonly -eq 1 ]; then
		echo "INFO: Patching .gwt.xml files such that only one GWT permutation needs to be compiled"
		for i in $GWT_XML_FILES; do
		    echo "INFO: Patching $i files such that only one GWT permutation needs to be compiled"
		    cp $i $i.bak
		    cat $i | sed -e 's/AllPermutations/SinglePermutation/' >$i.sed
		    mv $i.sed $i                
		done
	    else
		echo "INFO: Patching .gwt.xml files such that all GWT permutations are compiled"
		for i in $GWT_XML_FILES; do
		    echo "INFO: Patching $i files such that all GWT permutations are compiled"
		    cp $i $i.bak
		    cat $i | sed -e 's/SinglePermutation/AllPermutations/' >$i.sed
		    mv $i.sed $i
		done
	    fi
	else
	    echo "INFO: GWT Compilation disabled"
	    extra="$extra -Pdebug.no-gwt-compile"
	fi

	if [ $p2local -eq 1 ]; then
	    echo "INFO: Building and using local p2 repo"
	    #build local p2 repo
	    echo "Using following command (pwd: java/com.sap.sailing.targetplatform.base): mvn ${extra} -fae -s $MAVEN_SETTINGS $clean compile"
	    echo "Maven version used: `mvn --version`"
            echo "JAVA_HOME used: $JAVA_HOME"
	    (cd com.sap.$PROJECT_TYPE.targetplatform.base; mvn ${extra} -fae -s $MAVEN_SETTINGS $clean compile 2>&1 | tee -a $START_DIR/build.log)
	    # now get the exit status from mvn, and not that of tee which is what $? contains now
	    MVN_EXIT_CODE=${PIPESTATUS[0]}
	    echo "Maven exit code is $MVN_EXIT_CODE"
	    # Build AWS API; its local repo must exist for creating the local target definition in the next step
	    (cd com.amazon.aws.aws-java-api; ./createLocalAwsApiP2Repository.sh | tee -a $START_DIR/build.log)
	    # create local target definition
	    (cd com.sap.$PROJECT_TYPE.targetplatform/scripts; ./createLocalTargetDef.sh)
	    extra="$extra -Dp2-local" # activates the p2-target.local profile in java/pom.xml
	else
	    echo "INFO: Using remote p2 repos (http://p2.sapsailing.com/p2/sailing/ and http://p2.sapsailing.com/p2/aws-sdk/)"
        fi

    # back to root!
    cd $PROJECT_HOME

    if [ $testing -eq 0 ]; then
	    echo "INFO: Skipping tests"
	    extra="$extra -Dmaven.test.skip=true -DskipTests=true"
    else
        extra="$extra -DskipTests=false"
        # TODO: Think about http://maven.apache.org/surefire/maven-surefire-plugin/examples/fork-options-and-parallel-execution.html
        if [[ "$TESTCASE_TO_EXECUTE" != "" ]]; then
            # http://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html
            echo "Running only testcase $TESTCASE_TO_EXECUTE"
            extra="$extra -Dtest=$TESTCASE_TO_EXECUTE"
        fi
    fi

    if [ $android -eq 0 ] && [ $gwtcompile -eq 0 ] && [ $testing -eq 0 ]; then
        parallelexecution=1
        echo "INFO: Running build in parallel with 2.5*CPU threads"
        extra="$extra -T 2.5C"
    fi

    if [ $android -eq 1 ]; then
        if [[ $ANDROID_HOME == "" ]]; then
            echo "Environment variable ANDROID_HOME not found. Aborting."
            echo "Deactivate mobile build with parameter -a."
            exit 1
        fi
        echo "ANDROID_HOME=$ANDROID_HOME"
        PATH=$PATH:$ANDROID_HOME/tools/bin
        PATH=$PATH:$ANDROID_HOME/platform-tools
        SDK_MANAGER="$ANDROID_HOME/cmdline-tools/8.0/bin/sdkmanager"
        if [ \! -x "$SDK_MANAGER" ]; then
            SDK_MANAGER="$ANDROID_HOME/tools/bin/sdkmanager.bat"
        fi
        echo "SDK_MANAGER=${SDK_MANAGER}"
        echo "cmdline-tools:"
        ls -l "$ANDROID_HOME/cmdline-tools/"
        
        BUILD_TOOLS_VERSION=`grep "buildTools = " build.gradle | cut -d "\"" -f 2`
        echo "BUILD_TOOLS_VERSION=$BUILD_TOOLS_VERSION"
        TARGET_API_VERSION=`grep "targetSdk = " build.gradle | cut -d "=" -f 2 | sed 's/ //g'`
        echo "TARGET_API_VERSION=$TARGET_API_VERSION"
        echo "Updating Android SDK at ${ANDROID_HOME}"
        $SDK_MANAGER --update --sdk_root=${ANDROID_HOME} && yes | $SDK_MANAGER --licenses
        echo "Getting Android build-tools, platform-tools and platform ${TARGET_API_VERSION}"
        $SDK_MANAGER --sdk_root=${ANDROID_HOME} "build-tools;$BUILD_TOOLS_VERSION" "platform-tools" "platforms;android-$TARGET_API_VERSION" "tools"

        # TODO: make distinction available for gradle builds as well
        # Uncomment the following line for testing an artifact stages in the SAP-central Nexus system:
        # mobile_extra="-P -with-not-android-relevant -P with-mobile -P use-staged-third-party-artifacts -Dmaven.repo.local=${TMP}/temp_maven_repo"
        # Use the following line for regular builds with no staged Nexus artifacts:
        # mobile_extra="-P -with-not-android-relevant -P with-mobile"

        echo "Building apps with Gradle..."
        JAVA_HOME="${JAVA8_HOME}" ./gradlew build
        if [[ ${PIPESTATUS[0]} != 0 ]]; then
            exit 100
        fi
        JAVA_HOME="${JAVA8_HOME}" ./gradlew assemble
        if [[ ${PIPESTATUS[0]} != 0 ]]; then
            exit 100
        fi
        if [ $testing -eq 1 ]; then
            echo "Starting JUnit tests..."
            # ./gradlew test | tee -a $START_DIR/build.log
            # if [[ ${PIPESTATUS[0]} != 0 ]]; then
            #    exit 103
            # fi
            # TODO find a way that the emulator test is stable in hudson
            # adb emu kill
            # echo "Downloading image (sys-img-${ANDROID_ABI}-android-${TEST_API})..." | tee -a $START_DIR/build.log
            # echo yes | "$ANDROID" update sdk $ANDROID_OPTIONS --filter sys-img-${ANDROID_ABI}-android-${TEST_API} --no-ui --force --all > /dev/null
            # echo no | "$ANDROID" create avd --name ${AVD_NAME} --target android-${TEST_API} --abi ${ANDROID_ABI} --force -p ${START_DIR}/emulator
            # echo "Starting emulator..." | tee -a $START_DIR/build.log
            # emulator -avd ${AVD_NAME} -no-skin -no-audio -no-window &
            # echo "Waiting for startup..." | tee -a $START_DIR/build.log
            # adb wait-for-device
            # sleep 60
            # $PROJECT_HOME/configuration/androidWaitForEmulator.sh
            # if [[ $? != 0 ]]; then
            #     adb emu kill
            #     "$ANDROID" delete avd --name ${AVD_NAME}
            #     exit 102
            # fi
            # adb shell input keyevent 82 &
            # ./gradlew deviceCheck connectedCheck | tee -a $START_DIR/build.log
            # if [[ ${PIPESTATUS[0]} != 0 ]]; then
            #   adb emu kill
            #   "$ANDROID" delete avd --name ${AVD_NAME}
            #   exit 101
            # fi
            # adb emu kill
            # "$ANDROID" delete avd --name ${AVD_NAME}
        fi
    fi

    if [ $java -eq 1 ]; then
        if [ $reporting -eq 1 ]; then
            echo "INFO: Activating reporting"
            extra="$extra -Dreportsdirectory=$PROJECT_HOME/target/surefire-reports"
        fi
    
        # make sure to honour the service configuration
        # needed to make sure that tests use the right servers
        if [ -n "${MONGODB_HOST}" ]; then
          APP_PARAMETERS="-Dmongo.host=${MONGODB_HOST} ${APP_PARAMETERS}"
        fi
        if [ -n "${MONGODB_PORT}" ]; then
          APP_PARAMETERS="-Dmongo.port=${MONGODB_PORT} ${APP_PARAMETERS}"
        fi
        if [ -n "${EXPEDITION_PORT}" ]; then
          APP_PARAMETERS="-Dexpedition.udp.port=${EXPEDITION_PORT} ${APP_PARAMETERS}"
        fi
        if [ -n "${REPLICATION_HOST}" ]; then
          APP_PARAMETERS="-Dreplication.exchangeHost=${REPLICATION_HOST} ${APP_PARAMETERS}"
        fi
        if [ -n "${REPLICATION_CHANNEL}" ]; then
          APP_PARAMETERS="-Dreplication.exchangeName=${REPLICATION_CHANNEL} ${APP_PARAMETERS}"
        fi
    
        extra="$extra -P with-not-android-relevant,!with-mobile"
        if [ $gwtcompile -eq 1 ]; then
          if [ "${BUILD_GWT_FORK}" = "1" ]; then
            echo "Building and installing forked GWT version..."
	    JAVA_HOME="${JAVA8_HOME}" `dirname $0`/install-gwt "${PROJECT_HOME}"
          else
            echo "Downloading and installing forked GWT version..."
            `dirname $0`/install-gwt-from-fork-releases https://github.com/SAP/gwt-forward-serialization-rpc https://github.com/SAP/gwt-maven-plugin-forward-serialization-rpc 2.11.1 .
          fi
        fi
        echo "Using following command: mvn $extra -DargLine=\"$APP_PARAMETERS\" -fae -s $MAVEN_SETTINGS $clean install"
        echo "Maven version used: `mvn --version`"
        echo "JAVA_HOME used: $JAVA_HOME"
	export MAVEN_OPTS="-Xmx4g"
        mvn $extra -DargLine="$APP_PARAMETERS" -fae -s $MAVEN_SETTINGS $clean install 2>&1 | tee -a $START_DIR/build.log
        # now get the exit status from mvn, and not that of tee which is what $? contains now
        MVN_EXIT_CODE=${PIPESTATUS[0]}
        echo "Maven exit code is $MVN_EXIT_CODE"
    
        if [ $reporting -eq 1 ]; then
            echo "INFO: Generating reports"
            echo "Using following command: mvn $extra -DargLine=\"$APP_PARAMETERS\" -fae -s $MAVEN_SETTINGS surefire-report:report-only"
            mvn $extra -DargLine="$APP_PARAMETERS" -fae -s $MAVEN_SETTINGS surefire-report:report-only 2>&1 | tee $START_DIR/reporting.log
            tar -xzf configuration/surefire-reports-resources.tar.gz
            echo "INFO: Reports generated in $PROJECT_HOME/target/site/surefire-report.html"
            echo "INFO: Be sure to check the result of the actual BUILD run!"
        fi
    
        cd $PROJECT_HOME/java
        if [ $gwtcompile -eq 1 ]; then
    	# Now move back the backup .gwt.xml files before they were (maybe) patched
    	echo "INFO: restoring backup copies of .gwt.xml files after they has been patched before"
    	for i in $GWT_XML_FILES; do
    	    mv -v $i.bak $i
    	done
        fi
    
        if [ $MVN_EXIT_CODE -eq 0 ]; then
    	echo "Build complete. Do not forget to install product..."
        else
            echo "Build had errors. Maven exit status was $MVN_EXIT_CODE"
        fi
        exit $MVN_EXIT_CODE
    fi
fi

if [[ "$@" == "install" ]] || [[ "$@" == "all" ]]; then

    if [ $suppress_confirmation -eq 0 ]; then
        read -s -n1 -p "Currently branch $active_branch is active and I will deploy to $ACDIR. Do you want to proceed with $@ (y/N): " answer
        case $answer in
        "Y" | "y") echo "Continuing";;
        *) echo "Aborting..."
           exit 1;;
        esac
    fi

    if [ ! -d $ACDIR ]; then
        echo "Could not find directory $ACDIR - perhaps you are on a wrong branch?"
        exit 1
    fi

    # secure current state so that it can be reused if something goes wrong
    if [ -f "$ACDIR/backup-binaries.tar.gz" ]; then
        rm -f $ACDIR/backup-binaries.tar.gz
    fi

    tar cvzf $ACDIR/backup-binaries.tar.gz $ACDIR/plugins $ACDIR/configuration

    if [ ! -d "$ACDIR/plugins" ]; then
        mkdir $ACDIR/plugins
    fi

    if [ ! -d "$ACDIR/logs" ]; then
        mkdir $ACDIR/logs
    fi

    if [ ! -d "$ACDIR/tmp" ]; then
        mkdir $ACDIR/tmp
    fi

    if [ ! -d "$ACDIR/configuration" ]; then
        mkdir $ACDIR/configuration
    fi

    if [ ! -d "${JETTY_CONFIG_DIR}/etc" ]; then
        mkdir -p ${JETTY_CONFIG_DIR}/etc
    fi

    cd $ACDIR

    rm -rf $ACDIR/plugins/*.*
    rm -rf $ACDIR/org.eclipse.*
    rm -rf $ACDIR/configuration/org.eclipse.*

    # always overwrite start and stop scripts as they
    # should never contain any custom logic
    cp -v $PROJECT_HOME/java/target/start $ACDIR/
    cp -v $PROJECT_HOME/java/target/stop $ACDIR/
    cp -v $PROJECT_HOME/java/target/status $ACDIR/
    cp -v $PROJECT_HOME/java/target/configuration/JavaSE-11.profile $ACDIR/
    cp -v $PROJECT_HOME/java/target/refreshInstance.sh $ACDIR/
    cp -v $PROJECT_HOME/java/target/stopReplicating.sh $ACDIR/
    cp -v $PROJECT_HOME/java/target/generateMailProperties.sh $ACDIR/
    cp -v $PROJECT_HOME/java/target/udpmirror $ACDIR/
    cp -v $PROJECT_HOME/java/target/http2udpmirror $ACDIR

    # overwrite configurations that should never be customized and belong to the build
    cp -v $p2PluginRepository/configuration/config.ini configuration/
    cp -v $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty.xml ${JETTY_CONFIG_SUBDIR}/etc
    cp -v $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty-http.xml ${JETTY_CONFIG_SUBDIR}/etc
    cp -v $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty-deploy.xml ${JETTY_CONFIG_SUBDIR}/etc
    cp -v $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/realm.properties ${JETTY_CONFIG_SUBDIR}/etc

    if [ ! -f "$ACDIR/env.sh" ]; then
        cp -v $PROJECT_HOME/java/target/env.sh $ACDIR/
        cp -v $PROJECT_HOME/java/target/env-default-rules.sh $ACDIR/
        cp -v $PROJECT_HOME/java/target/defineReverseProxyMappings.sh $ACDIR/
        cp -v $PROJECT_HOME/java/target/configuration/monitoring.properties $ACDIR/configuration/
        cp -v $PROJECT_HOME/java/target/configuration/mail.properties $ACDIR/configuration/
        cp -v $PROJECT_HOME/java/target/configuration/logging.properties $ACDIR/configuration/
    fi

    cp -r -v $p2PluginRepository/configuration/org.eclipse.equinox.simpleconfigurator configuration/
    cp -v $p2PluginRepository/plugins/*.jar plugins/

    cp -rv $PROJECT_HOME/configuration/native-libraries $ACDIR/

    # Make sure this script is up2date at least for the next run
    cp -v $PROJECT_HOME/configuration/buildAndUpdateProduct.sh $ACDIR/

    # make sure to read the information from env.sh
    . $ACDIR/env.sh
    . $ACDIR/env-default-rules.sh

    create_version_info_files

    # When a server is installed using this script
    # then we no longer define important options in
    # config.ini because this is generated by product
    # installer. Instead we inject these properties
    # using system properties. This works because
    # context.getProperty() searches for system properties
    # if it can't find them in config.ini (framework config)
    sed -i "/mongo.host/d" "$ACDIR/configuration/config.ini"
    sed -i "/mongo.port/d" "$ACDIR/configuration/config.ini"
    sed -i "/expedition.udp.port/d" "$ACDIR/configuration/config.ini"
    sed -i "/replication.exchangeName/d" "$ACDIR/configuration/config.ini"
    sed -i "/replication.exchangeHost/d" "$ACDIR/configuration/config.ini"
    sed -i "s/^.*jetty.port.*$/<Set name=\"port\"><Property name=\"jetty.port\" default=\"$SERVER_PORT\"\/><\/Set>/g" "${JETTY_CONFIG_DIR}/etc/jetty-http.xml"

    echo "I have read the following configuration from $ACDIR/env.sh:"
    echo "SERVER_NAME: $SERVER_NAME"
    echo "SERVER_PORT: $SERVER_PORT"
    echo "MEMORY: $MEMORY"
    echo "TELNET_PORT: $TELNET_PORT"
    echo "MONGODB_PORT: $MONGODB_PORT"
    echo "MONGODB_HOST: $MONGODB_HOST"
    echo "EXPEDITION_PORT: $EXPEDITION_PORT"
    echo "REPLICATION_HOST: $REPLICATION_HOST"
    echo "REPLICATION_CHANNEL: $REPLICATION_CHANNEL"
    echo ""

    echo "I did NOT overwrite env.sh if it already existed! Use the refreshInstance.sh script to update your configuration!"
    echo "Installation complete. You may now start the server using ./start"
fi

if [[ "$@" == "remote-deploy" ]]; then
    SERVER=$TARGET_SERVER_NAME
    echo "Will deploy server $SERVER"

    SSH_CMD="ssh $REMOTE_SERVER_LOGIN"
    SCP_CMD="scp -r"

    REMOTE_HOME=`ssh $REMOTE_SERVER_LOGIN 'echo $HOME/servers'`
    REMOTE_SERVER="$REMOTE_HOME/$SERVER"

    if [ $suppress_confirmation -eq 0 ]; then
        read -s -n1 -p "I will deploy the current GIT branch to $REMOTE_SERVER_LOGIN:$REMOTE_SERVER. Is this correct (y/n)? " answer
        case $answer in
        "Y" | "y") OK=1;;
        *) echo "Aborting... nothing has been changed on remote server!"
        exit;;
        esac
    fi

    $SSH_CMD "test -d $REMOTE_SERVER/plugins"
    if [[ $? -eq 1 ]]; then
        echo "Did not find directory $REMOTE_SERVER/plugins - assuming empty server that needs to be initialized! Using data from $PROJECT_HOME"

        $SSH_CMD "mkdir -p $REMOTE_SERVER/plugins"
        $SSH_CMD "mkdir -p $REMOTE_SERVER/logs"
        $SSH_CMD "mkdir -p $REMOTE_SERVER/tmp"
        $SSH_CMD "mkdir -p $REMOTE_SERVER/${JETTY_CONFIG_SUBDIR}/etc"

        $SCP_CMD $p2PluginRepository/configuration/config.ini $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/configuration/
        $SCP_CMD $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty.xml $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/${JETTY_CONFIG_SUBDIR}/etc
        $SCP_CMD $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty-http.xml $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/${JETTY_CONFIG_SUBDIR}/etc
        $SCP_CMD $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/jetty-deploy.xml $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/${JETTY_CONFIG_SUBDIR}/etc
        $SCP_CMD $PROJECT_HOME/java/target/${JETTY_CONFIG_SUBDIR}/etc/realm.properties $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/${JETTY_CONFIG_SUBDIR}/etc
        $SCP_CMD $PROJECT_HOME/java/target/configuration/monitoring.properties $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/configuration/
        $SCP_CMD $PROJECT_HOME/java/target/configuration/mail.properties $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/configuration/

        $SCP_CMD $PROJECT_HOME/java/target/env.sh $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/env-default-rules.sh $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/defineReverseProxyMappings.sh $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/start $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/stop $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/status $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/refreshInstance.sh $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/stopReplicating.sh $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/generateMailProperties.sh $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/udpmirror $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/

        $SCP_CMD $PROJECT_HOME/java/target/http2udpmirror $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/
        $SCP_CMD $PROJECT_HOME/java/target/configuration/logging.properties $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/configuration/
    fi

    echo ""
    echo "Starting deployment to $REMOTE_HOME/$SERVER..."

    $SSH_CMD "rm -rf $REMOTE_SERVER/plugins/*.*"
    $SSH_CMD "rm -rf $REMOTE_SERVER/org.eclipse*.*"
    $SSH_CMD "rm -rf $REMOTE_SERVER/configuration/org.eclipse*.*"

    $SCP_CMD $p2PluginRepository/configuration/org.eclipse.equinox.simpleconfigurator $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/configuration/
    $SCP_CMD $p2PluginRepository/plugins/*.jar $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/plugins/

    echo "$VERSION_INFO System: remotedly-deployed" > /tmp/version-remote-deploy.txt
    $SCP_CMD /tmp/version-remote-deploy.txt $REMOTE_SERVER_LOGIN:$REMOTE_SERVER/${JETTY_CONFIG_SUBDIR}/version.txt
    rm /tmp/version-remote-deploy.txt

    echo "Deployed successfully. I did NOT change any configuration (no env.sh or config.ini or jetty.xml adaption), only code!"

    if [ $suppress_confirmation -eq 0 ]; then
        read -s -n1 -p "Do you want me to restart the remote server (y/n)? " answer
        case $answer in
        "Y" | "y") OK=1;;
        *) echo "Aborting... deployment should be ready by now!"
        exit;;
        esac

        echo ""
        $SSH_CMD "cd $REMOTE_SERVER && bash -l -c $REMOTE_SERVER/stop"
        $SSH_CMD "cd $REMOTE_SERVER && bash -l -c $REMOTE_SERVER/start"

        echo "Restarted remote server. Please check."
    fi
fi

if [[ "$@" == "deploy-startpage" ]]; then
    TARGET_DIR_STARTPAGE=$ACDIR/tmp/jetty-0.0.0.0-8889-bundlefile-_-any-/webapp/
    read -s -n1 -p "Copying $PROJECT_HOME/java/com.sap.$PROJECT_TYPE.www/index.html to $TARGET_DIR_STARTPAGE - is this ok (y/n)?" answer
    case $answer in
    "Y" | "y") OK=1;;
    *) echo "Aborting... nothing has been changed for startpage!"
    exit;;
    esac

    cp $PROJECT_HOME/java/com.sap.$PROJECT_TYPE.www/index.html $TARGET_DIR_STARTPAGE
    echo "OK"
fi

echo "Operation finished at `date`"
