#!/bin/bash
# Fetches the CPU load data from the leaderboards of the Sailing Analytics server
# whose base URL is provided as the first argument. The second argument has to be
# a bearer token that authenticates a user to read the /cpu endpoint. Example usage:
#
#  getCPUOfLeaderboards.sh https://www.sapsailing.com AUxGpA83JB294m/f17/MgiYhdRB3xoDCYd+rLc398Ls=
#
BASE_URL="${1}"
BEARER_TOKEN="${2}"
FIRST=1
JSON_OUTPUT='['`curl -L "${BASE_URL}/sailingserver/api/v1/leaderboards" 2>/dev/null | jq '.[]' | while read lb; do
url="${BASE_URL}/sailingserver/api/v1/leaderboards/$( echo -n "${lb}" | sed -e 's/^"//' -e 's/"$//' | jq -sRr @uri )/cpu"
  if [ "${FIRST}" != "1" ]; then
    echo -n ", "
  else
    FIRST=0
  fi
  if [ -z "${BEARER_TOKEN}" ]; then
    LEADERBOARD_CPU_JSON=$( curl -H 'X-SAPSSE-Forward-Request-To: master' -L "${url}" 2>/dev/null )
  else
    LEADERBOARD_CPU_JSON=$( curl -H 'X-SAPSSE-Forward-Request-To: master' -H 'Authorization: Bearer '${BEARER_TOKEN} -L "${url}" 2>/dev/null )
  fi
  if ! echo "${LEADERBOARD_CPU_JSON}" | grep -q "Subject does not have permission \[LEADERBOARD:UPDATE"; then
      echo -n "{\"leaderboard\": \"$( echo -n "${lb}" | sed -e 's/^"//' -e 's/"$//' )\", \"cpu\": ${LEADERBOARD_CPU_JSON}}"
  else
    FIRST=1
  fi
done`']'
echo "${JSON_OUTPUT}" | jq -C 'sort_by(.cpu.totals.cpuTotalMillis) | reverse' | less -R
