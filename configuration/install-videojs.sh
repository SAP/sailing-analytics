#!/bin/bash
# Usage: install-videojs.sh [ {tag-name } ]
# If no {tag-name} parameter is specified, the latest tip of the default branch
# from the git@github.com:videojs/video.js.git repository will be used.
# Example: install-videojs.sh v12.1.2
if [ -z "${TMP}" ]; then
    TMP=/tmp
fi
VIDEOJS="$( mktemp -d ${TMP}/videojsXXXXX )"
TARGET="$( realpath $( dirname ${0} ))/../java/com.sap.sailing.gwt.ui/js/video-js"
echo "Will copy git files to ${TARGET}"
echo "Cloning videojs to ${VIDEOJS} ..."
cd "${VIDEOJS}"
if [ -n "${1}" ]; then
    ADDITIONAL_CLONE_ARGS="--branch ${1}"
fi
git clone git@github.com:videojs/video.js.git --depth 1 ${ADDITIONAL_CLONE_ARGS}
cd video.js
npm install -g npm-run-all
npm install
npm run build
cd "${TARGET}"
echo "Now in directory $(pwd)"
for i in $( find * -prune -type f \( -name '*.js' -o -name '*.css' \) ); do
    cp "${VIDEOJS}/video.js/dist/${i}" "${i}"
done
cp "${VIDEOJS}/video.js/dist/lang/*" ./lang
cp "${VIDEOJS}/video.js/dist/font/*" ./font
rm -rf "${VIDEOJS}"
