#!/bin/bash
# Uses a gradle build in java/com.amazon.aws.aws-java-api to obtain the JARs for the AWS Java API (SDK)
# into the lib/ folder there, then generates a corresponding .classpath,  META-INF/MANIFEST.MF, and build.properties file.
# Then, the java/com.amazon.aws.aws-java-api.updatesite/features/aws-sdk/feature.xml is adjusted to reflect the current version.
# The update site is then built locally into java/com.amazon.aws.aws-java-api.updatesite/target/repository which can then
# be tested with the local target platform definition. If everything works fine, the uploadAwsApiRepositoryToServer.sh script
# can be used to update the repository contents at p2.sapsailing.com with the updated local target platform repository contents.
LIB=lib
JAR=`which jar`
if [ "$JAR" = "" ]; then
  JAR="$JAVA_HOME/bin/jar"
fi
CLASSPATH_FILE=".classpath"
MANIFEST_FILE="MANIFEST.MF"
BUILD_PROPERTIES_FILE="build.properties"
WORKSPACE=`realpath \`dirname $0\`/../..`
UPDATE_SITE_PROJECT=${WORKSPACE}/java/com.amazon.aws.aws-java-api.updatesite
SSE_RUNTIME_FEATURE_XML=${WORKSPACE}/java/com.sap.sse.feature.runtime/feature.xml
FEATURE_XML=${UPDATE_SITE_PROJECT}/features/aws-sdk/feature.xml
SITE_XML=${UPDATE_SITE_PROJECT}/site.xml
TARGET_DEFINITION="${WORKSPACE}/java/com.sap.sailing.targetplatform/definitions/race-analysis-p2-remote.target"
WRAPPER_BUNDLE="${WORKSPACE}/java/com.amazon.aws.aws-java-api"
cd ${WRAPPER_BUNDLE}
echo "Creating .project file from dot_project template to allow for Eclipse workspace import..."
cp dot_project .project
echo "Downloading libraries..."
rm -rf ${LIB}/*
JAVA_HOME=${JAVA8_HOME} ${WORKSPACE}/gradlew downloadLibs
cd ${LIB}
VERSION=`ls -1 aws-core-*.jar | grep -v -- -sources | sed -e 's/aws-core-\([.0-9]*\)\.jar/\1/' | sort | tail -n 1`
echo VERSION=${VERSION}
LIBS=`ls -1 | grep -v -- -sources\.jar`
echo "Generating the .classpath file..."
echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<classpath>" >${WRAPPER_BUNDLE}/${CLASSPATH_FILE}
for l in ${LIBS}; do
  SOURCES_JAR=`basename $l .jar`-sources.jar
  if [ -f ${SOURCES_JAR} ]; then
    SOURCEPATH=" sourcepath=\"${LIB}/${SOURCES_JAR}\""
  else
    SOURCEPATH=""
  fi
  echo "        <classpathentry exported=\"true\" kind=\"lib\" path=\"${LIB}/${l}\"${SOURCEPATH}/>" >>${WRAPPER_BUNDLE}/${CLASSPATH_FILE}
done
echo "        <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8\"/>
        <classpathentry kind=\"con\" path=\"org.eclipse.pde.core.requiredPlugins\"/>
        <classpathentry kind=\"output\" path=\"bin\"/>
</classpath>" >>${WRAPPER_BUNDLE}/${CLASSPATH_FILE}
echo "Patching version ${VERSION} into pom.xml..."
# exclude SNAPSHOT version used for the parent pom; only match the explicit SDK version
sed -i -e 's/<version>\([0-9.]*\)<\/version>/<version>'${VERSION}'<\/version>/' ${WRAPPER_BUNDLE}/pom.xml
echo "Generating the META-INF/MANIFEST.MF file..."
mkdir -p "${WRAPPER_BUNDLE}/META-INF"
echo -n "Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: aws-java-api
Bundle-SymbolicName: com.amazon.aws.aws-java-api
Bundle-Version: ${VERSION}
Bundle-Vendor: Amazon
Bundle-RequiredExecutionEnvironment: JavaSE-1.8
Bundle-ClassPath: " >${WRAPPER_BUNDLE}/META-INF/${MANIFEST_FILE}
for l in ${LIBS}; do
  echo -n "lib/${l},
 " >>${WRAPPER_BUNDLE}/META-INF/${MANIFEST_FILE}
done
echo "   ...determining exported packages from libs to generate Export-Package in manifest..."
echo -n ".
Automatic-Module-Name: com.amazon.aws.aws-java-api
Export-Package:" >>${WRAPPER_BUNDLE}/META-INF/${MANIFEST_FILE}
PACKAGES=$(for l in ${LIBS}; do
  "$JAR" tvf ${l} | grep "\.class\>" | sed -e 's/^.* \([^ ]*\)$/\1/' -e 's/\/[^/]*\.class\>//' | grep "^software/amazon"
done | sort -u | tr / . )
for p in `echo "${PACKAGES}" | while read i; do echo $i | sed -e 's/^\([-a-zA-Z0-9_.]*\)\>.*$/\1/'; done | head --lines=-1`; do
   echo " ${p}," >>${WRAPPER_BUNDLE}/META-INF/${MANIFEST_FILE}
done
for p in `echo "${PACKAGES}" | while read i; do echo $i | sed -e 's/^\([-a-zA-Z0-9_.]*\)\>.*$/\1/'; done | tail --lines=1`; do
   echo " ${p}" >>${WRAPPER_BUNDLE}/META-INF/${MANIFEST_FILE}
done
echo "Generating build.properties..."
echo -n "bin.includes = META-INF/,\\
               ." >${WRAPPER_BUNDLE}/${BUILD_PROPERTIES_FILE}
for l in ${LIBS}; do
  echo -n ",\\
               lib/${l}" >>${WRAPPER_BUNDLE}/${BUILD_PROPERTIES_FILE}
done
echo >>${WRAPPER_BUNDLE}/${BUILD_PROPERTIES_FILE}
echo "Building the wrapper bundle..."
cd ..
echo "NOT USING ${extra} arguments for Maven to avoid using local target definition already for building local repo"
echo "In folder $(pwd) using: mvn clean install"
JAVA_HOME=${JAVA8_HOME} mvn --batch-mode clean install
mkdir -p ${UPDATE_SITE_PROJECT}/plugins/aws-sdk
rm -rf ${UPDATE_SITE_PROJECT}/plugins/aws-sdk/*
# Note: the JAR ends up in target/ because the SDK project does not use any parent pom, so
# it doesn't inherit the output directory specification "bin/" from its parent, and
# target/ is the default.
mv target/com.amazon.aws.aws-java-api-${VERSION}.jar ${UPDATE_SITE_PROJECT}/plugins/aws-sdk/
echo "Unpacking source bundles..."
cd ${LIB}
for l in *-sources.jar; do
  "${JAR}" xvf $l software
done
echo "Creating sources JAR..."
SOURCE_JAR_MANIFEST=source-manifest.mf
echo "Manifest-Version: 1.0
Bundle-SymbolicName: com.amazon.aws.aws-java-api.source
Bundle-Name: AWS SDK Sources
Bundle-Version: ${VERSION}
Eclipse-SourceBundle: com.amazon.aws.aws-java-api;version=\"${VERSION}\"
Bundle-ManifestVersion: 2" >${SOURCE_JAR_MANIFEST}
"${JAR}" cvfm com.amazon.aws.aws-java-api.source_${VERSION}.jar ${SOURCE_JAR_MANIFEST} software/
rm ${SOURCE_JAR_MANIFEST}
echo "Removing extracted sources..."
rm -rf software
mv com.amazon.aws.aws-java-api.source_${VERSION}.jar ${UPDATE_SITE_PROJECT}/plugins/aws-sdk
cd ${UPDATE_SITE_PROJECT}
echo "Patching update site's feature.xml..."
sed -i -e 's/^\( *\)version="[0-9.]*"/\1version="'${VERSION}'"/' ${FEATURE_XML}
echo "Patching com.sap.sse.feature.runtime's feature.xml..."
NEW_SSE_RUNTIME_FEATURE_XML_CONTENT=$( cat "${SSE_RUNTIME_FEATURE_XML}" |  awk -v VERSION=${VERSION} '
/id="com.amazon.aws.aws-java-api"/ { IN_PLUGIN="true"; }
/id="com.amazon.aws.aws-java-api.source"/ { IN_PLUGIN_SOURCE="true"; }
{ if (IN_PLUGIN=="true" && $0 ~ / *version=".*"/) { print "         version=\"" VERSION "\""; IN_PLUGIN="false"; } else
if (IN_PLUGIN_SOURCE=="true" && $0 ~ / *version=".*"/) { print "         version=\"" VERSION "\""; IN_PLUGIN_SOURCE="false"; } else print $0; }
' )
echo "${NEW_SSE_RUNTIME_FEATURE_XML_CONTENT}" >"${SSE_RUNTIME_FEATURE_XML}"
echo "Patching update site's site.xml..."
sed -i -e 's/com.amazon.aws.aws-java-api\(\.source\)\?_\([0-9.]*\)\.jar/com.amazon.aws.aws-java-api\1_'${VERSION}'.jar/' -e '/feature url=/s/version="[0-9.]*"/version="'${VERSION}'"/' ${SITE_XML}
echo "Building update site..."
echo "In folder $(pwd) using: mvn ${extra} clean install"
JAVA_HOME=${JAVA8_HOME} mvn ${extra} clean install
echo "Patching SDK version ${VERSION} in target platform definition ${TARGET_DEFINITION}..."
sed -i -e 's/<unit id="com.amazon.aws.aws-java-api.feature.group" version="[0-9.]*"\/>/<unit id="com.amazon.aws.aws-java-api.feature.group" version="'${VERSION}'"\/>/' ${TARGET_DEFINITION}
echo "You may test your target platform locally by creating race-analysis-p2-local.target by running the script createLocalTargetDef.sh."
echo "You can also try a Hudson build with the -v option, generating and using the local target platform during the build."
echo "In this case, start with an unpatched remote target platform (race-analysis-p2-remote.target) and an unpatched"
echo "java/com.sap.sse.feature.runtime/feature.xml so that running this script during the Hudson build can resolve"
echo "the target platform."
echo "When all this works, commit and push the patched to race-analysis-p2-remote.target and feature.xml"
echo "and update the P2 repository at p2.sapsailing.com using the script uploadAwsApiRepositoryToServer.sh."
