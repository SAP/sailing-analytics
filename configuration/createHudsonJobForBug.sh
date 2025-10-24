#!/bin/bash
if [ $# -eq 0 ]; then
    echo "$0 <bugid>"
    echo ""
    echo
    echo "Constructs a Hudson job for the given bugid"
    echo "Example: $0 4221"
    echo "Builds a Hudson job for bug branch bug4221, linking to the Bugzilla bug and copying a release"
    echo "from Github to https://releases.sapsailing.com if the Github Actions Workflow built one"
    exit 2
fi

BUG_ID="$1"

CONFIGFILE=$(mktemp mylocalconfigXXXX.xml)
RESPONSE_HEADERS=$(mktemp responseheadersXXXX)
HUDSON_BASE_URL=https://hudson.sapsailing.com
BUGZILLA_BASE=https://bugzilla.sapsailing.com/bugzilla
COPY_TEMPLATE_JOB=CopyTemplate
OS_FOR_GSED="darwin"
read -p "Username: " USERNAME
read -s -p "Password: " PASSWORD
echo
COPY_TEMPLATE_CONFIG_URL="$HUDSON_BASE_URL/job/$COPY_TEMPLATE_JOB/config.xml"
curl -s -X GET $COPY_TEMPLATE_CONFIG_URL -u "$USERNAME:$PASSWORD" -o "$CONFIGFILE"

# On macosx is gnu-sed needed
if [[ "$OSTYPE" == *"$OS_FOR_GSED"* ]]; then
  echo "Using gsed"
  gsed -i'' -e 's|<description>..*</description>|<description>This is the CI job for \&lt;a href=\&quot;'$BUGZILLA_BASE'/show_bug.cgi?id='$BUG_ID'\&quot;\&gt;Bug '$BUG_ID'\&lt;/a\&gt;. See its latest \&lt;a href=\&quot;/userContent/measurements.html?job=bug'$BUG_ID'\&quot;\&gt;quality and performance measurements here.\&lt;/a\&gt;</description>|' -e 's|<disabled>true</disabled>|<disabled>false</disabled>|' "$CONFIGFILE"
else
  sed -i -e 's|<description>..*</description>|<description>This is the CI job for \&lt;a href=\&quot;'$BUGZILLA_BASE'/show_bug.cgi?id='$BUG_ID'\&quot;\&gt;Bug '$BUG_ID'\&lt;/a\&gt;. See its latest \&lt;a href=\&quot;/userContent/measurements.html?job=bug'$BUG_ID'\&quot;\&gt;quality and performance measurements here.\&lt;/a\&gt;</description>|' -e 's|<disabled>true</disabled>|<disabled>false</disabled>|' "$CONFIGFILE"
fi

# On macosx is gnu-sed needed
if [[ "$OSTYPE" == *"$OS_FOR_GSED"* ]]; then
  echo "Using gsed"
  gsed -i'' -n -e ':loop
/<command>/b InCommand
p
$b
N
D
b loop
:InCommand
s/BRANCH/'bug${BUG_ID}'/g
/<\/command>/p
/<\/command>/b End
N
b InCommand
:End
' "$CONFIGFILE"
else
  sed -i -n -e ':loop
/<command>/b InCommand
p
$b
N
D
b loop
:InCommand
s/BRANCH/'bug${BUG_ID}'/g
/<\/command>/p
/<\/command>/b End
N
b InCommand
:End
' "$CONFIGFILE"
fi

if [[ "$OSTYPE" == *"$OS_FOR_GSED"* ]]; then
  gsed -i'' -e '/<branches>$/{
  N
  N
  s|<name>[^<]*</name>|<name>bug'$BUG_ID'</name>|
  }' "$CONFIGFILE"
else
  sed -i -e '/<branches>$/{
  N
  N
  s|<name>[^<]*</name>|<name>bug'$BUG_ID'</name>|
  }' "$CONFIGFILE"
fi
curl -D "$RESPONSE_HEADERS" -s -XPOST "$HUDSON_BASE_URL/createItem?name=bug$BUG_ID" -u "$USERNAME:$PASSWORD" --data-binary "@$CONFIGFILE" -H "Content-Type:text/xml" >/dev/null 2>/dev/null
RESPONSE_CODE=$(cat "$RESPONSE_HEADERS" | head -n 1 | cut -d ' ' -f2 )
if [[ "$RESPONSE_CODE" =~ 2.. ]]; then
  echo "Find your new, enabled Hudson job at $HUDSON_BASE_URL/job/bug$BUG_ID/"
else
  echo "Error. HTTP response code $RESPONSE_CODE. Did the job already exist?"
fi
rm "$CONFIGFILE"
rm "$RESPONSE_HEADERS"
