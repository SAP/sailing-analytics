#!/bin/bash
# Usage: ${0} {MONGO-URI} {MONTHS}
# Example: findUsersWithSessionInMonths.sh "mongodb://localhost/winddb?replicaSet=rs0" 12
echo '
var cutoff = new Date();
cutoff.setMonth(cutoff.getMonth() - '${2}');

var activeUsers = db.SESSIONS.distinct(
  "SESSION_ATTRIBUTES.SESSION_ATTRIBUTE_VALUE.SESSION_PRINCIPAL_REALM_VALUE",
  { SESSION_START_TIMESTAMP: { $gte: cutoff } }
).flat();

db.USERS.find(
  {
    EMAIL_VALIDATED: true,
    EMAIL: { $exists: true, $ne: "" },
    NAME: { $in: activeUsers },
    $or: [
      { DID_OPT_OUT_OF_FEATURE_AND_COMMUNITY_EMAILS: { $exists: false } },
      { DID_OPT_OUT_OF_FEATURE_AND_COMMUNITY_EMAILS: false }
    ]
  },
  { EMAIL: 1, FULLNAME: 1, NAME: 1, _id: 0 }
).forEach(function(u) {
  var name = (u.FULLNAME && u.FULLNAME.trim()) || u.NAME || "user";
  print(u.EMAIL + "," + name);
})' | mongosh "${1}"
