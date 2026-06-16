#!/bin/bash
# Call with three arguments:
#  - the name of the branch for which you would like
#    to download the latest workflow run's build artifacts
#  - the Github PAT (personal access token)
#  - the Github repository {owner}/{repo}, such as SAP/sailing-analytics
# If found, the build.log.zip and test-result.zip files
# will be downloaded to the current working directory.
# The script will exit with status 0 if the workflow's
# conclusion was "success" and the artifacts downloaded
# fine. If the workflow's conclusion was not "success",
# an exit status of 1 is returned. If the downloads fail,
# an exit status of 2 is returned.
BRANCH="${1}"
BEARER_TOKEN="${2}"
GITHUB_REPOSITORY="${3}"
UNIX_TIME=$( date +%s )
UNIX_DATE=$( date --iso-8601=second )
UNIX_TIME_YESTERDAY=$(( UNIX_TIME - 10*24*3600 )) # look back ten days in time, trying to catch even re-runs of older jobs
DATE_YESTERDAY=$( date --iso-8601=second -d @${UNIX_TIME_YESTERDAY} )
HEADERS_FILE=$( mktemp headersXXXXX )
NEXT_PAGE="https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/runs?created=${DATE_YESTERDAY/+/%2B}..${UNIX_DATE/+/%2B}&per_page=100"
ARTIFACTS_JSON=""
LATEST_RUN_STARTED_AT="0000-00-00T00:00:00Z"

# Function to check a workflow run and all its previous attempts
# Arguments: $1 = workflow run JSON
# Sets LAST_WORKFLOW_FOR_BRANCH and LATEST_RUN_STARTED_AT if a better match is found
check_run_and_previous_attempts() {
  local RUN_JSON="${1}"
  local CURRENT_RUN="${RUN_JSON}"

  while [ -n "${CURRENT_RUN}" ] && [ "${CURRENT_RUN}" != "null" ]; do
    local RUN_STATUS=$( echo "${CURRENT_RUN}" | jq -r '.status' )
    local RUN_NAME=$( echo "${CURRENT_RUN}" | jq -r '.name' )
    local RUN_HEAD_BRANCH=$( echo "${CURRENT_RUN}" | jq -r '.head_branch' )

    # Check if this run matches our criteria
    if [ "${RUN_STATUS}" == "completed" ] && [ "${RUN_NAME}" == "release" ]; then
      if [[ "${RUN_HEAD_BRANCH}" == ${BRANCH}* ]] || [[ "${RUN_HEAD_BRANCH}" == releases/${BRANCH}* ]]; then
        local RUN_STARTED_AT=$( echo "${CURRENT_RUN}" | jq -r '.run_started_at' )
        echo "Found completed run (or previous attempt) started at ${RUN_STARTED_AT}"
        if [[ "${RUN_STARTED_AT}" > "${LATEST_RUN_STARTED_AT}" ]]; then
          echo "This is later than the latest run so far (${LATEST_RUN_STARTED_AT})"
          LAST_WORKFLOW_FOR_BRANCH="${CURRENT_RUN}"
          LATEST_RUN_STARTED_AT="${RUN_STARTED_AT}"
        fi
      fi
    fi
    # Check for previous attempt
    local PREVIOUS_ATTEMPT_URL=$( echo "${CURRENT_RUN}" | jq -r '.previous_attempt_url // empty' )
    if [ -n "${PREVIOUS_ATTEMPT_URL}" ]; then
      echo "Checking previous attempt at ${PREVIOUS_ATTEMPT_URL} ..."
      CURRENT_RUN=$( curl --silent -L "${PREVIOUS_ATTEMPT_URL}" 2>/dev/null )
      # Validate we got a proper response
      if [ -z "${CURRENT_RUN}" ] || [ "$( echo "${CURRENT_RUN}" | jq -r '.id // empty' )" == "" ]; then
        echo "Could not fetch previous attempt, stopping chain"
        break
      fi
    else
      break
    fi
  done
}

# Now go through the pages as long as we have a non-empty NEXT_PAGE URL and find the completed "release" workflow that was started last
while [ -n "${NEXT_PAGE}" ]; do
  echo "Trying page ${NEXT_PAGE} ..."
  # Get the artifacts URL of the last workflow run triggered by a branch push for ${BRANCH}:
  NEXT_PAGE_CONTENTS=$( curl -D "${HEADERS_FILE}" --silent -L "${NEXT_PAGE}" 2>/dev/null )
  # Get all workflow runs that match our branch criteria (completed or not, we'll check status in the function)
  MATCHING_RUNS=$( echo "${NEXT_PAGE_CONTENTS}" | jq -c '.workflow_runs | map(select(.name == "release" and ((.head_branch | startswith("'${BRANCH}'")) or (.head_branch | startswith("releases/'${BRANCH}'"))))) | .[]' 2>/dev/null )
  if [ -n "${MATCHING_RUNS}" ]; then
    # Process each matching run
    while IFS= read -r RUN_JSON; do
      if [ -n "${RUN_JSON}" ] && [ "${RUN_JSON}" != "null" ]; then
        echo "Checking run and its previous attempts..."
        check_run_and_previous_attempts "${RUN_JSON}"
      fi
    done <<< "${MATCHING_RUNS}"
  else
    echo "Found no runs for branch ${BRANCH} on page"
  fi
  NEXT_PAGE=$( grep "^link: .*; rel=\"next\"" "${HEADERS_FILE}" | sed -e 's/^.*<\([^>]*\)>; rel="next".*$/\1/' )
done
ARTIFACTS_URL=$( echo "${LAST_WORKFLOW_FOR_BRANCH}" | jq -r '.artifacts_url' )
CONCLUSION=$( echo "${LAST_WORKFLOW_FOR_BRANCH}" | jq -r '.conclusion' )
ARTIFACTS_JSON=$( curl --silent "${ARTIFACTS_URL}" )
rm "${HEADERS_FILE}"
if [ -z "${ARTIFACTS_JSON}" ]; then
  echo "Workflow run or artifacts not found"
  exit 1
fi
echo "Using run started at ${LATEST_RUN_STARTED_AT}"
BUILD_LOG_URL=$( echo "${ARTIFACTS_JSON}" | jq -r '.artifacts | map(select(.name == "build.log"))[0].archive_download_url' )
if [ -z "${BUILD_LOG_URL}" ]; then
  echo "build.log artifact not found"
  exit 2
fi
echo "Downloading build.log ZIP from ${BUILD_LOG_URL}"
curl --silent --output build.log.zip -L "${BUILD_LOG_URL}"
TEST_RESULTS_URL=$( echo "${ARTIFACTS_JSON}" | jq -r '.artifacts | map(select(.name == "test-results"))[0].archive_download_url' )
if [ -z "${TEST_RESULTS_URL}" ]; then
  echo "test-results artifact not found"
  exit 2
fi
echo "Downloading test-results ZIP from ${TEST_RESULTS_URL}"
curl --silent --output test-results.zip -L "${TEST_RESULTS_URL}"
if [ "${CONCLUSION}" != "success" ]; then
  exit 1
fi
