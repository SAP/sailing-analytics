#!/bin/bash
VERSION=$( curl https://download.eclipse.org/jetty/updates/jetty-bundles-9.x/ | grep "/jetty/updates/jetty-bundles-9.x" | sed -e 's/^.*> \?\(9\.4\.[0-9]*\.v[0-9]*\)<.*$/\1/' )
echo "Found version ${VERSION}"
P2_URL="https://download.eclipse.org/jetty/updates/jetty-bundles-9.x/${VERSION}/"
TMP_FOLDER="${TMP}/jetty-${VERSION}"
rm -rf "${TMP_FOLDER}"
eclipse -nosplash -verbose -application org.eclipse.equinox.p2.metadata.repository.mirrorApplication -source https://download.eclipse.org/jetty/updates/jetty-bundles-9.x/${VERSION}/ -destination file:${TMP_FOLDER}
eclipse -nosplash -verbose -application org.eclipse.equinox.p2.artifact.repository.mirrorApplication -source https://download.eclipse.org/jetty/updates/jetty-bundles-9.x/${VERSION}/ -destination file:${TMP_FOLDER}
echo "Uploading the ${TMP_FOLDER} folder to trac@sapsailing.com:p2-repositories/jetty-${VERSION} ..."
ssh trac@sapsailing.com "rm -rf p2-repositories/jetty-${VERSION}"
scp -rp ${TMP_FOLDER} trac@sapsailing.com:p2-repositories/jetty-${VERSION}
OLD_VERSION=$( cat ../definitions/race-analysis-p2-remote.target | grep '\(<unit id="org\.eclipse\.jetty\.bundles\.f\.feature\.group" version="\)\([^"]*\)\("\/>\)' | sed -e 's/\(<unit id="org\.eclipse\.jetty\.bundles\.f\.feature\.group" version="\)\([^"]*\)\("\/>\).*$/\2/' )
echo "Found old version ${OLD_VERSION}"
echo "Updating com.sap.sse.feature.runtime/feature.xml from Jetty bundles version ${OLD_VERSION} to ${VERSION} ..."
sed -i -e 's/version="'${OLD_VERSION}'"/version="'${VERSION}'"/' ../../com.sap.sse.feature.runtime/feature.xml
echo "Updating target platform definition..."
sed -i -e 's/\(<unit id="org\.eclipse\.jetty\.bundles\.f\.\(source\.\)\?feature\.group" version="\)[^"]*\("\/>\)/\1'${VERSION}'\3/' -e 's/\(<repository location="https:\/\/p2\.sapsailing\.com\/p2\/\)jetty.*\(\/"\/>\)/\1jetty-'${VERSION}'\2/' ../definitions/race-analysis-p2-remote.target
echo "Your target platform definition now points to https://p2.sapsailing.com/p2/jetty-${VERSION} for the Jetty bundles. Now please update the org.eclipse.jetty.osgi.boot.jsp[.source] and org.eclipse.jetty.apache-jsp[.source] bundles in java/com.sap.sailing.targetplatform.base/plugins/target-base and try a build with the -v parameter, using a local target platform. If this works, run the createLocalBaseP2repository.sh and uploadRepositoryToServer.sh scripts to produce an updated p2 sailing repository. Check for the new versions of apache-jsp and osgi.boot.jsp here https://repo1.maven.org/maven2/org/eclipse/jetty/apache-jsp/ and here https://repo1.maven.org/maven2/org/eclipse/jetty/osgi/jetty-osgi-boot/, respectively."
rm -rf "${TMP_FOLDER}"
