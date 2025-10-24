#!/bin/bash
echo "
"
BODY=$( cat )
REF=$( echo "${BODY}" | jq -r '.ref' )
PUSHER=$( echo "${BODY}" | jq -r '.pusher.email' )
logger -t github-cgi "ref is $REF, pusher was $PUSHER"
# For testing:
#if [ "${PUSHER}" = "your-email@example.com" -a "${REF}" = "refs/heads/translation" ]; then
# The filter in case this is to be used for alternative Github at github.tools.sap:
#if [ "${PUSHER}" = "tmsatsls+github.tools.sap_service-tip-git@sap.com" -a "${REF}" = "refs/heads/translation" ]; then
# The filter for github.com:
if [ "${PUSHER}" = "tmsatsls+github.com_service-tip-git@sap.com" -a "${REF}" = "refs/heads/translation" ]; then
  echo "Identified a push to refs/heads/translation by ${PUSHER}."
  echo "Fetching translation branch from github and pushing it to ssh://trac@sapsailing.com/home/trac/git"
  logger -t github-cgi "fetching translation branch from github and pushing it to ssh://trac@sapsailing.com/home/trac/git"
  cd /home/wiki/gitwiki
  sudo -u wiki git fetch github translation:translation 2>&1
  sudo -u wiki git push origin translation:translation 2>&1
else
  echo "Either pusher was not tmsatsls+github.com_service-tip-git@sap.com or ref was not refs/heads/translation. Not triggering."
fi
