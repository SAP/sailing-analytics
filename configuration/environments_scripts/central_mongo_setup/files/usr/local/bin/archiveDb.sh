#!/bin/bash
# Uses mongodump / mongorestore to copy a database from the live replica set to
# the archive database. Afterwards, a comparison is made using the mongohash
# command
# Usage: archiveDb.sh [ -y ] <dbname>

if [ "$1" = "-y" ]; then
  CONFIRMATION="y"
  shift
else
  CONFIRMATION=""
fi

DB=$1
DUMPBASEDIR=/var/lib/mongodb/slow/.dump
mkdir -p "${DUMPBASEDIR}"

if [ "$CONFIRMATION" = "" ]; then
  echo -n "This will drop the database $DB in the archive DB before importing. Are you sure (y/n)? "
  read CONFIRMATION
  if [ "$CONFIRMATION" != "y" ]; then
    exit 2;
  fi
fi

pushd "$DUMPBASEDIR"
mongodump --port 10203 --db $DB
# Drop the entire DB in the archive DB to ensure there are no left-over collections
mongosh --eval "use $DB" --eval "db.dropDatabase()" --port 10202
mongorestore --noIndexRestore --drop --port 10202 dump
rm -rf dump/$DB
popd
ORIGINAL_HASH=$(`dirname $0`/mongohash -p 10203 -d $DB)
ARCHIVE_HASH=$(`dirname $0`/mongohash -p 10202 -d $DB)
if [ -n "${ORIGINAL_HASH}" -a -n "${ARCHIVE_HASH}" -a $ORIGINAL_HASH = $ARCHIVE_HASH ]; then
  echo Hashes of old and new are equal: $ORIGINAL_HASH and $ARCHIVE_HASH. Dropping $DB and ${DB}-replica in live DB
  mongosh --eval "use ${DB}" --eval "db.dropDatabase()" --eval "use ${DB}-replica" --eval "db.dropDatabase()" "mongodb://localhost:10203/${DB}?replicaSet=live"
  echo "Don't forget to switch your server's env.sh so it has MONGODB_URI=\"mongodb://dbserver.internal.sapsailing.com:10202/${DB}\""
  exit 0
else
  echo "Hashes of old and new are empty or differ: ${ORIGINAL_HASH} and ${ARCHIVE_HASH}"
  exit 1
fi
rm -rf ${DUMPBASEDIR}"
