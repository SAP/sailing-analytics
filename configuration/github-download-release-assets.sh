#!/bin/bash
# Download release tar ball and release-notes.txt into the current working directory
# and echo the .tar.gz file name to the standard output if found. If no release is found
# for the prefix specified, nothing it sent to the standard output. The exit status will
# always be 0.
#
# Usage:
#   ./github-download-release-assets.sh {BEARER_TOKEN} {release-name-prefix} {repository-name}
# For example:
#  ./github-download-release-assets.sh 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 main- SAP/sailing-analytics
# which will download the latest release tar.gz and release-notes.txt of the main branch (main-xxxxxxxxxxx).
# Note the "-" at the end of the "main-" prefix specifier; this way we're making name
# clashes with releases whose name happens to start with "main" unlikely. This
# also suggests you shouldn't name releases "main-abcde-xxxxxxxxxxxx" because they would
# produce false matches for the "main-" prefix.
BEARER_TOKEN="${1}"
RELEASE_NAME_PREFIX="${2}"
GITHUB_REPOSITORY="${3}"
RELEASES=$( curl -L -H 'Authorization: Bearer '${BEARER_TOKEN} https://api.github.com/repos/${GITHUB_REPOSITORY}/releases 2>/dev/null )
RELEASE_NOTES_TXT_ASSET_ID=$( echo "${RELEASES}" | jq -r 'sort_by(.published_at) | reverse | map(select(.name | startswith("'${RELEASE_NAME_PREFIX}'")))[0].assets[] | select(.content_type=="text/plain").id' 2>/dev/null)
if [ "$?" -ne "0" ]; then
  echo "No release with prefix ${RELEASE_NAME_PREFIX} found. Not trying to download/upload anything." >&2
else
  RELEASE_TAR_GZ_ASSET_ID=$( echo "${RELEASES}" | jq -r 'sort_by(.published_at) | reverse | map(select(.name | startswith("'${RELEASE_NAME_PREFIX}'")))[0].assets[] | select(.content_type=="application/x-tar").id' )
  RELEASE_FULL_NAME=$( echo "${RELEASES}" | jq -r 'sort_by(.published_at) | reverse | map(select(.name | startswith("'${RELEASE_NAME_PREFIX}'")))[0].assets[] | select(.content_type=="application/x-tar").name' | sed -e 's/\.tar\.gz$//')
  RELEASE_NAME=$( echo ${RELEASE_FULL_NAME} | sed -e 's/^\(.*\)-\([0-9]*\)$/\1/' )
  RELEASE_TIMESTAMP=$( echo ${RELEASE_FULL_NAME} | sed -e 's/^\(.*\)-\([0-9]*\)$/\2/' )
  echo "Found release ${RELEASE_FULL_NAME} with name ${RELEASE_NAME} and time stamp ${RELEASE_TIMESTAMP}, notes ID is ${RELEASE_NOTES_TXT_ASSET_ID}, tarball ID is ${RELEASE_TAR_GZ_ASSET_ID}" >&2
  RELEASE_TAR_GZ_FILE_NAME="${RELEASE_FULL_NAME}.tar.gz"
  curl -o "${RELEASE_TAR_GZ_FILE_NAME}" -L -H 'Accept: application/octet-stream' -H 'Authorization: Bearer '${BEARER_TOKEN} 'https://api.github.com/repos/'${GITHUB_REPOSITORY}'/releases/assets/'${RELEASE_TAR_GZ_ASSET_ID}
  curl -o release-notes.txt -L -H 'Accept: application/octet-stream' -H 'Authorization: Bearer '${BEARER_TOKEN} 'https://api.github.com/repos/'${GITHUB_REPOSITORY}'/releases/assets/'${RELEASE_NOTES_TXT_ASSET_ID}
  echo "${RELEASE_TAR_GZ_FILE_NAME}"
fi
