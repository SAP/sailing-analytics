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
JSON_OUTPUT='['`curl -L "${BASE_URL}/sailingserver/api/v1/leaderboards" 2>/dev/null | jq -r '.[]' | while read lb; do
  url="${BASE_URL}/sailingserver/api/v1/leaderboards/$( echo -n "${lb}" | jq -sRr @uri )/cpu"
  if [ "${FIRST}" != "1" ]; then
    echo -n ", "
  else
    FIRST=0
  fi
  LEADERBOARD_CPU_JSON=$( curl -L -H 'Authorization: Bearer '${BEARER_TOKEN} "${url}" 2>/dev/null )
  echo -n "{\"leaderboard\": \"${lb}\", \"cpu\": ${LEADERBOARD_CPU_JSON}}"
done`']'
echo "${JSON_OUTPUT}"
