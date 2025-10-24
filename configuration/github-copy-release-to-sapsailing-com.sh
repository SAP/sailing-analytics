#!/bin/bash
# Download release tar ball and release-notes.txt into the current working directory
# and echo the .tar.gz file name to the standard output if found. If no release is found
# for the prefix specified, nothing it sent to the standard output. The exit status will
# always be 0.
#
# Usage:
#   ./github-copy-release-to-sapsailing-com.sh {BEARER_TOKEN} {release-name-prefix}
# For example:
#  ./github-copy-release-to-sapsailing-com.sh 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 main-
#
# which will download the latest build of the main branch (main-xxxxxxxxxxx) and upload
# it to releases.sapsailing.com.
# Note the "-" at the end of the "main-" prefix specifier; this way we're making name
# clashes with releases whose name happens to start with "main" unlikely. This
# also suggests you shouldn't name releases "main-abcde-xxxxxxxxxxxx" because they would
# produce false matches for the "main-" prefix.
BEARER_TOKEN="${1}"
RELEASE_NAME_PREFIX="${2}"
RELEASE_TAR_GZ_FILE_NAME=$( `dirname "${0}"`/github-download-release-assets.sh "${BEARER_TOKEN}" "${RELEASE_NAME_PREFIX}" SAP/sailing-analytics )
if [ "${RELEASE_TAR_GZ_FILE_NAME}" != "" ]; then
  RELEASE_NAME=$( echo ${RELEASE_TAR_GZ_FILE_NAME} | sed -e 's/^\(.*\)-\([0-9]*\).tar.gz$/\1/' )
  RELEASE_TIMESTAMP=$( echo ${RELEASE_TAR_GZ_FILE_NAME} | sed -e 's/^\(.*\)-\([0-9]*\).tar.gz$/\2/' )
  RELEASE_FULL_NAME="${RELEASE_NAME}-${RELEASE_TIMESTAMP}"
  echo "Found release ${RELEASE_FULL_NAME} with name ${RELEASE_NAME} and time stamp ${RELEASE_TIMESTAMP}" >&2
  FOLDER=releases/${RELEASE_FULL_NAME}
  ssh trac@sapsailing.com mkdir -p ${FOLDER}
  scp ${RELEASE_FULL_NAME}.tar.gz trac@sapsailing.com:${FOLDER}
  scp release-notes.txt trac@sapsailing.com:${FOLDER}
  rm ${RELEASE_TAR_GZ_FILE_NAME} release-notes.txt
fi
