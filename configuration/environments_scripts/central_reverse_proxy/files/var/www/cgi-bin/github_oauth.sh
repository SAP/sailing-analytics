#!/bin/bash
echo "Content-Type: text/html"
echo ""
# Read secrets:
. ~/secrets
# ==== CONFIG for OAuth App wiki.sapsailing.com: ====
CLIENT_ID="${GITHUB_OAUTH_CLIENT_ID}"
CLIENT_SECRET="${GITHUB_OAUTH_CLIENT_SECRET}"
REDIRECT_URI="https://git.sapsailing.com/cgi-bin/github_oauth.sh"
# ===================================================
STATES_FILE=/tmp/github_oauth_states
# Parse QUERY_STRING for code
QUERY="$QUERY_STRING"
CODE=""
STATE_PARAM=""
IFS='&' read -ra KV <<< "$QUERY"
for pair in "${KV[@]}"; do
  key="${pair%%=*}"
  val="${pair#*=}"
  if [ "$key" = "code" ]; then
    CODE="$val"
  elif [ "$key" = "state" ]; then
    STATE_PARAM="$val"
  fi
done
# Checking for code parameter; if not found, start the flow
if [ -z "$CODE" ]; then
  # No code → show login page
  STATE=$(openssl rand -hex 16)
  echo "${STATE}" >>"${STATES_FILE}"
  AUTH_URL="https://github.com/login/oauth/authorize?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&scope=read:user,user:email&state=${STATE}"
  cat <<EOF
<html><head><title>GitHub OAuth Login</title></head>
<body>
  <h2>Login with GitHub</h2>
  <p><a href="${AUTH_URL}">Authorize this app to access your GitHub account</a></p>
</body></html>
EOF
  exit 0
else
  if grep -q "${STATE_PARAM}" "${STATES_FILE}"; then
    sed -i '/'${STATE_PARAM}'/d' "${STATES_FILE}"
    # Got a code → exchange it for an access token
    TOKEN_JSON=$(curl -s -X POST https://github.com/login/oauth/access_token \
      -H "Accept: application/json" \
      -d "client_id=${CLIENT_ID}" \
      -d "client_secret=${CLIENT_SECRET}" \
      -d "code=${CODE}" \
      -d "redirect_uri=${REDIRECT_URI}" )
    ACCESS_TOKEN=$(echo "$TOKEN_JSON" | jq -r '.access_token')
    if [ "$ACCESS_TOKEN" = "null" ] || [ -z "$ACCESS_TOKEN" ]; then
      cat <<EOF
    <html><body>
    <h3>Failed to obtain access token.</h3>
    <pre>${TOKEN_JSON}</pre>
    </body></html>
EOF
      exit 1
    else
      # Use token to get user info
      USER_JSON=$(curl -s -H "Authorization: token ${ACCESS_TOKEN}" -H "Accept: application/vnd.github+json" https://api.github.com/user)
      LOGIN=$(echo "$USER_JSON" | jq -r '.login')
      cat <<EOF
      <html><head><title>GitHub OAuth Success</title></head>
      <body>
	<h2>Welcome, ${LOGIN}!</h2>
	<p>The state param ${STATE_PARAM} was found in our ${STATES_FILE}:</p>
	<pre>$(cat "${STATES_FILE}")</pre>
	<p>You have successfully authenticated with GitHub.</p>
	<p>Your token JSON was:</p>
	<pre>${TOKEN_JSON}</pre>
	<p><strong>Access Token:</strong> ${ACCESS_TOKEN}</p>
	<p>You can now use this token to make API calls on behalf of this user.</p>
	<pre>${USER_JSON}</pre>
      </body></html>
EOF
    fi
  else
    # The ${STATE_PARAM} was not found in ${STATES_FILE}, so this may be an attack
    cat <<EOF
  <html><body>
  <h3>Failed to find state; attack?</h3>
  <pre>${STATE_PARAM}</pre>
  </body></html>
EOF
    exit 2
  fi
fi
