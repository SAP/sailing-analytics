#!/bin/bash
GIT_ROOT=/home/wiki/gitwiki
# Downloads all TracTrac event data based on ${GIT_ROOT}/configuration/tractrac-json-urls
# into the target directory (specified as $1) for those event URLs whose specific folder
# does not yet exist in the target directory.
TARGET_DIR="${1}"
JSON_URLS_FILE="${GIT_ROOT}/configuration/tractrac-json-urls"
for i in `cat "${JSON_URLS_FILE}"`; do
  EVENT_DB="$( basename $( dirname ${i} ) )"
  if [ -d "${TARGET_DIR}/${EVENT_DB}" ]; then
    echo "Directory for event ${EVENT_DB} already found. Not downloading again. Use"
    echo "  ${GIT_ROOT}/configuration/downloadTracTracEvent ${i} ${TARGET_DIR}"
    echo "to force an update."
  else
    echo "Did not find directory for event ${EVENT_DB} yet in ${TARGET_DIR}. Downloading..."
    ${GIT_ROOT}/configuration/downloadTracTracEvent "${i}" "${TARGET_DIR}"
  fi
done
