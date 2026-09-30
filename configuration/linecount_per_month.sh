#!/bin/bash
LINECOUNT="$( mktemp --tmpdir linecountXXXXX.sh )"
cp "$( dirname ${0} )/linecount.sh" "${LINECOUNT}"
chmod a+x "${LINECOUNT}"
for y in $( seq 2011 2026 ); do
    for m in $( seq 01 12 ); do
        d=$( printf '%04d-%02d-01\n' ${y} ${m} )
        commit=$( git rev-list -n 1 --first-parent --before=${d} main 2>/dev/null )
        if [ -n "${commit}" ]; then
            echo -n "$d "
            git reset --hard >/dev/null 2>&1
            git checkout -f ${commit} >/dev/null 2>&1
            "${LINECOUNT}"
            git reset --hard >/dev/null 2>&1
        fi
    done
done
