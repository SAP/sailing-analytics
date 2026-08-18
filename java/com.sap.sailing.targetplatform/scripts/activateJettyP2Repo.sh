#!/bin/bash
#
# After creating the p2 repository with createJettyP2RepoFromMavenCentral.sh,
# this script updates all references in the workspace.
#
# Usage:
#   ./activateJettyP2Repo.sh [VERSION]
#
#   VERSION defaults to 9.4.58.v20250814 if not supplied.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VERSION="${1:-9.4.58.v20250814}"

# ---------------------------------------------------------------------------
# Determine the old version from the target definition
# ---------------------------------------------------------------------------
OLD_VERSION=$(grep '<unit id="org\.eclipse\.jetty\.bundles\.f\.feature\.group" version="' \
  "${SCRIPT_DIR}/../definitions/race-analysis-p2-remote.target" \
  | sed -e 's/.*version="\([^"]*\)".*/\1/')

echo "=== Upgrading Jetty from ${OLD_VERSION} to ${VERSION} ==="

# ---------------------------------------------------------------------------
# 1. Update target platform definitions
# ---------------------------------------------------------------------------
echo ""
echo "--- Updating target platform definitions ---"
for TARGET_FILE in \
  "${SCRIPT_DIR}/../definitions/race-analysis-p2-remote.target" \
  "${SCRIPT_DIR}/../definitions/race-analysis-p2-local.target"
do
  if [ -f "${TARGET_FILE}" ]; then
    echo "  ${TARGET_FILE}"
    sed -i \
      -e 's/\(<unit id="org\.eclipse\.jetty\.bundles\.f\.\(source\.\)\?feature\.group" version="\)[^"]*\("\/>\)/\1'"${VERSION}"'\3/' \
      -e 's|\(<repository location="https://p2\.sapsailing\.com/p2/\)jetty[^"]*\("\/>\)|\1jetty-'"${VERSION}"'\2|' \
      "${TARGET_FILE}"
  fi
done

# ---------------------------------------------------------------------------
# 2. Update feature.xml files with new Jetty version
# ---------------------------------------------------------------------------
echo ""
echo "--- Updating feature.xml files ---"

RUNTIME_FEATURE="${SCRIPT_DIR}/../../com.sap.sse.feature.runtime/feature.xml"
if [ -f "${RUNTIME_FEATURE}" ]; then
  echo "  ${RUNTIME_FEATURE}"
  sed -i -e 's/version="'"${OLD_VERSION}"'"/version="'"${VERSION}"'"/' "${RUNTIME_FEATURE}"
fi

TARGET_BASE_FEATURE="${SCRIPT_DIR}/../../com.sap.sailing.targetplatform.base/features/target-base/feature.xml"
if [ -f "${TARGET_BASE_FEATURE}" ]; then
  echo "  ${TARGET_BASE_FEATURE}"
  sed -i -e 's/version="'"${OLD_VERSION}"'"/version="'"${VERSION}"'"/' "${TARGET_BASE_FEATURE}"
fi

# ---------------------------------------------------------------------------
# 3. Update apache-jsp and jetty-osgi-boot-jsp jars in target-base
# ---------------------------------------------------------------------------
echo ""
echo "--- Updating JSP bundles in target-base plugins ---"
TARGET_BASE_PLUGINS="${SCRIPT_DIR}/../../com.sap.sailing.targetplatform.base/plugins/target-base"

echo "  Removing old versions..."
rm -f "${TARGET_BASE_PLUGINS}/apache-jsp-${OLD_VERSION}.jar"
rm -f "${TARGET_BASE_PLUGINS}/apache-jsp-${OLD_VERSION}-sources.jar"
rm -f "${TARGET_BASE_PLUGINS}/jetty-osgi-boot-jsp-${OLD_VERSION}.jar"
rm -f "${TARGET_BASE_PLUGINS}/jetty-osgi-boot-jsp-${OLD_VERSION}-sources.jar"

MAVEN_CENTRAL="https://repo1.maven.org/maven2"

echo "  Downloading new versions..."
wget -q -O "${TARGET_BASE_PLUGINS}/apache-jsp-${VERSION}.jar" \
  "${MAVEN_CENTRAL}/org/eclipse/jetty/apache-jsp/${VERSION}/apache-jsp-${VERSION}.jar"
wget -q -O "${TARGET_BASE_PLUGINS}/apache-jsp-${VERSION}-sources.jar" \
  "${MAVEN_CENTRAL}/org/eclipse/jetty/apache-jsp/${VERSION}/apache-jsp-${VERSION}-sources.jar"
wget -q -O "${TARGET_BASE_PLUGINS}/jetty-osgi-boot-jsp-${VERSION}.jar" \
  "${MAVEN_CENTRAL}/org/eclipse/jetty/osgi/jetty-osgi-boot-jsp/${VERSION}/jetty-osgi-boot-jsp-${VERSION}.jar"
wget -q -O "${TARGET_BASE_PLUGINS}/jetty-osgi-boot-jsp-${VERSION}-sources.jar" \
  "${MAVEN_CENTRAL}/org/eclipse/jetty/osgi/jetty-osgi-boot-jsp/${VERSION}/jetty-osgi-boot-jsp-${VERSION}-sources.jar"

echo "  Adding to git..."
git -C "${TARGET_BASE_PLUGINS}" add "*.jar"

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
echo ""
echo "=== DONE ==="
echo ""
echo "Your target platform now references jetty-${VERSION}."
echo "Next steps:"
echo "  1. Rebuild the local base p2 repository:  ./createLocalBaseP2repository.sh"
echo "  2. Upload it:  ./uploadRepositoryToServer.sh"
echo "  3. Reload the target platform in your IDE"
