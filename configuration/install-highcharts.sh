#!/bin/bash
# Usage: install-highcharts.sh [ {tag-name } ]
# If no {tag-name} parameter is specified, the latest tip of the default branch
# from the git@github.com:highcharts/highcharts-dist.git repository will be used.
# Example: install-highcharts.sh v12.1.2
if [ -z "${TMP}" ]; then
    TMP=/tmp
fi
HIGHCHARTS="$( mktemp -d ${TMP}/highchartsXXXXX )"
TARGET="$( realpath $( dirname ${0} ))/../java/com.sap.sse.gwt/src/com/sap/sse/gwt/resources/highcharts"
echo "Will copy git files to ${TARGET}"
echo "Cloning highcharts-dist to $HIGHCHARTS ..."
cd "${HIGHCHARTS}"
if [ -n "${1}" ]; then
    ADDITIONAL_CLONE_ARGS="--branch ${1}"
fi
git clone git@github.com:highcharts/highcharts-dist.git --depth 1 ${ADDITIONAL_CLONE_ARGS}
cd "${TARGET}"
echo "Now in directory $(pwd)"
for i in $( find . -type f ); do
    cp "${HIGHCHARTS}/highcharts-dist/${i}" "${i}"
done
rm -rf "${HIGHCHARTS}"
