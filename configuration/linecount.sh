#!/bin/sh
all=$( wc --total=only -l `find com.sap.* */com.sap.* -type f \( -name '*.java' -o -name '*.gwt.xml' -o -name '*.ini' -o -name '*.html' -o -name '*.css' -o -name '*.gss' \) \! -ipath '*/generated/*' \! -ipath '*/gen/*' \! -ipath '*/bin/*' \! -ipath '*/generated-sources/*' \! -ipath '*/.generated/*' \! -name R.java \! -ipath './java/com.sap.sailing.gwt.ui/com.sap.sailing.gwt.*' 2>/dev/null` )
gwt_files=`find *.gwt.* */*.gwt.* -type f \( -name '*.java' -o -name '*.gwt.xml' -o -name '*.ini' -o -name '*.html' -o -name '*.css' -o -name '*.gss' \) \! -ipath '*/generated/*' \! -ipath '*/gen/*' \! -ipath '*/bin/*' \! -ipath '*/generated-sources/*' \! -ipath '*/.generated/*' \! -name R.java \! -ipath './java/com.sap.sailing.gwt.ui/com.sap.sailing.gwt.*' 2>/dev/null`
if [ -n "${gwt_files}" ]; then
    gwt=$( wc --total=only -l ${gwt_files} )
else
    gwt=0
fi
echo "${gwt} ${all}"
