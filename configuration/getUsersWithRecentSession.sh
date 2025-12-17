#!/bin/bash
# A script that can extract users with a session with last access time at or
# after the ISO time stamp provided as argument #2, reading from the MongoDB
# provided as argument #1. The relevant collections are SESSIONS and USERS.
#
# Example:
#   getUsersWithRecentSession.sh "mongodb://mongo1.internal.sapsailing.com/security_service?replicaSet=live" 2025-01-01T00:00:00Z
#
# Will output a JSON array containing user records as object with fields NAME, FULLNAME,
# COMPANY, LOCAL, EMAIL, and EMAIL_VALIDATED
#
# To restrict the output to those whose e-mail address was validated, use something
# like:
#   getUsersWithRecentSession.sh ... | jq '[.[] | select(.EMAIL_VALIDATED==true)]
#
MONGO_URL="${1}"
SINCE_TIMESTAMP_ISO="${2}"
USERS_WITH_RECENT_SESSION_AS_JSON_ARRAY_OF_STRINGS=$( mongosh --quiet --eval '
EJSON.stringify(db.SESSIONS.aggregate([
  // Step 1: Filter documents by timestamp
  {
    $match: {
      SESSION_LAST_ACCESS_TIME: { $gt: ISODate("'${SINCE_TIMESTAMP_ISO}'") }
    }
  },
  // Step 2: Unwind SESSION_ATTRIBUTES to work with each outer object individually
  { $unwind: "$SESSION_ATTRIBUTES" },
  // Step 3: Keep only SESSION_ATTRIBUTES elements matching SESSION_ATTRIBUTE_NAME
  {
    $match: {
      "SESSION_ATTRIBUTES.SESSION_ATTRIBUTE_NAME": "org.apache.shiro.subject.support.DefaultSubjectContext_PRINCIPALS_SESSION_KEY"
    }
  },
  // Step 4: Extract the inner string (assume innerArray has one element)
  {
    $addFields: {
      username: { $arrayElemAt: ["$SESSION_ATTRIBUTES.SESSION_ATTRIBUTE_VALUE.SESSION_PRINCIPAL_REALM_NAME", 0] }
    }
  },
  // Step 5: Keep only non-empty strings
  {
    $match: {
      username: { $nin: ["", null] }
    }
  },
  // Step 6: Project only the string if desired
  {
    $project: {
      _id: 0,
      username: 1
    }
  }
]).toArray())' "${MONGO_URL}" | jq '[.[].username] | unique' )
mongosh --quiet --eval "EJSON.stringify(db.USERS.find({NAME: {\$in: ${USERS_WITH_RECENT_SESSION_AS_JSON_ARRAY_OF_STRINGS}}}).toArray())" "${MONGO_URL}" | jq '[.[] | { NAME, FULLNAME, COMPANY, LOCALE, EMAIL, EMAIL_VALIDATED }]'

