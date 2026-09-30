// One-shot migration: remove/repair legacy "random=" nonce documents in
// CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED across every database of a
// replica set.
//
// Background
// ----------
// The current Java keying (TracTracConnectivityParamsHandler.getKey ->
// TracTracRaceTrackerImpl.getParamURLStrippedOfRandomParam) writes and deletes
// restore-queue documents using the `paramURL` STRIPPED of its `random=<nonce>`
// query parameter. Legacy documents that stored the RAW (nonce-bearing)
// paramURL can therefore never be matched by replaceOne/deleteOne, so they
// linger forever, inflate the restore queue, and are re-attempted on every
// server start. See the read-only classifier
// classify-connectivity-params-restore-nonce-twins.js for detection.
//
// What this script does, per database that has the collection:
//   - groups documents by (type, strippedParamURL);
//   - for each nonce-bearing doc:
//       * if a stripped TWIN exists in the same group  -> back up, then DELETE the raw doc;
//       * if NO twin exists (lonely)                   -> back up, then STRIP random= IN PLACE
//                                                          (updateOne $set paramURL := stripped);
//   - within a group of several lonely nonce docs that strip to the SAME key,
//     only the FIRST is stripped in place; the rest are treated as duplicates
//     (backed up, then DELETED) to avoid creating a duplicate stripped key.
//
// Safety
// ------
//   - DRY-RUN by default: prints the plan, writes NOTHING.
//   - Set APPLY=1 to execute. Only then is the per-DB backup collection
//     (<collection>_NONCE_BACKUP) created, and only in databases that actually
//     have CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED. Each raw doc is copied
//     into the backup collection BEFORE it is deleted or modified.
//   - Auto-discovers databases via listDatabases; a DB without the target
//     collection is skipped.
//
// Usage
// -----
//   Dry-run (default) against every DB in a replica set:
//     mongosh "<replica-set-uri>" configuration/migrate-connectivity-params-restore-strip-nonce.js
//   Apply:
//     APPLY=1 mongosh "<replica-set-uri>" configuration/migrate-connectivity-params-restore-strip-nonce.js
//   Repeat with each replica-set URI you need to cover.
//
// APPLY is read from the process environment (mongosh exposes it as a global
// `process` under Node). When unset/anything other than "1", the script is a
// dry run.

(function () {
    'use strict';

    const COLLECTION = 'CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED';
    const BACKUP_COLLECTION = COLLECTION + '_NONCE_BACKUP';
    const PARAM_URL = 'paramURL';
    const TYPE = 'type';
    const APPLY = (typeof process !== 'undefined' && process.env && process.env.APPLY === '1');

    // Faithful re-implementation of
    // TracTracRaceTrackerImpl.getParamURLStrippedOfRandomParam(URL): removes only
    // a query parameter whose NAME is "random" (case-insensitive); truncates a
    // value containing '=' at the first '=' (Java's split("=") uses nameValue[0]
    // and nameValue[1]); empty resulting query yields no '?'; path and #ref are
    // preserved, an explicit port kept, an absent one omitted; userinfo dropped.
    // Returns the original string when there is no query or it cannot be parsed.
    function stripRandomParam(paramUrlString) {
        let result;
        if (paramUrlString === null || paramUrlString === undefined) {
            result = paramUrlString;
        } else {
            let parsed;
            try {
                parsed = new URL(paramUrlString);
            } catch (e) {
                parsed = null;
            }
            if (parsed === null) {
                result = paramUrlString;
            } else {
                const rawQuery = parsed.search.startsWith('?') ? parsed.search.substring(1) : parsed.search;
                if (rawQuery === '') {
                    result = rebuild(parsed, '');
                } else {
                    const kept = [];
                    const queryParams = rawQuery.split('&');
                    for (let i = 0; i < queryParams.length; i++) {
                        const nameValue = queryParams[i].split('=');
                        if (nameValue[0].toLowerCase() !== 'random') {
                            let param = nameValue[0];
                            if (nameValue.length > 1) {
                                param = param + '=' + nameValue[1];
                            }
                            kept.push(param);
                        }
                    }
                    const joined = kept.length === 0 ? '' : ('?' + kept.join('&'));
                    result = rebuild(parsed, joined);
                }
            }
        }
        return result;
    }

    function rebuild(parsed, queryWithPrefix) {
        const protocol = parsed.protocol.replace(/:$/, '');
        const host = parsed.hostname;
        const port = parsed.port === '' ? '' : (':' + parsed.port);
        const path = parsed.pathname;
        const ref = (parsed.hash && parsed.hash.length > 1) ? parsed.hash : '';
        return protocol + '://' + host + port + path + queryWithPrefix + ref;
    }

    function hasRandomParam(paramUrlString) {
        let found = false;
        if (paramUrlString) {
            let parsed;
            try {
                parsed = new URL(paramUrlString);
            } catch (e) {
                parsed = null;
            }
            if (parsed !== null) {
                const rawQuery = parsed.search.startsWith('?') ? parsed.search.substring(1) : parsed.search;
                if (rawQuery !== '') {
                    const queryParams = rawQuery.split('&');
                    for (let i = 0; i < queryParams.length; i++) {
                        if (queryParams[i].split('=')[0].toLowerCase() === 'random') {
                            found = true;
                        }
                    }
                }
            }
        }
        return found;
    }

    function toJsonString(value) {
        return EJSON.stringify(value);
    }

    // Migrate a single database that is known to hold the target collection.
    // Returns a small stats object. Honours APPLY (dry-run when false).
    function migrateDatabase(targetDb) {
        const coll = targetDb.getCollection(COLLECTION);
        const total = coll.countDocuments();
        print('  ' + COLLECTION + ': ' + total + ' documents');
        // Group by (type, stripped paramURL). Only TracTrac-style docs carry a
        // paramURL; docs without one cannot bear a random= nonce and are ignored.
        const groups = {};
        const cursor = coll.find({}, { _id: 1, [TYPE]: 1, [PARAM_URL]: 1 });
        while (cursor.hasNext()) {
            const doc = cursor.next();
            const paramUrl = doc[PARAM_URL];
            if (paramUrl !== null && paramUrl !== undefined) {
                const type = doc[TYPE];
                const paramUrlString = paramUrl.toString();
                const stripped = stripRandomParam(paramUrlString);
                const key = (type === null || type === undefined ? '<no-type>' : type) + '\u0000' + stripped;
                if (!groups[key]) {
                    groups[key] = [];
                }
                groups[key].push({
                    id: doc._id,
                    type: type,
                    paramURL: paramUrlString,
                    hasRandom: hasRandomParam(paramUrlString),
                    stripped: stripped
                });
            }
        }
        // Decide an action for every nonce-bearing doc.
        const deletes = []; // {id, reason, doc-loaded-later}
        const strips = [];  // {id, from, to}
        Object.keys(groups).forEach(function (key) {
            const members = groups[key];
            const nonceMembers = members.filter(function (m) { return m.hasRandom; });
            if (nonceMembers.length > 0) {
                const twinExists = members.some(function (m) { return !m.hasRandom && m.paramURL === m.stripped; });
                if (twinExists) {
                    // Every nonce doc here is a redundant raw copy: delete them all.
                    nonceMembers.forEach(function (nm) {
                        deletes.push({ id: nm.id, reason: 'stripped twin exists', paramURL: nm.paramURL });
                    });
                } else {
                    // Lonely group: strip the FIRST in place, delete any further
                    // nonce docs that would strip to the same key (would duplicate).
                    let stripped = false;
                    nonceMembers.forEach(function (nm) {
                        if (!stripped) {
                            strips.push({ id: nm.id, from: nm.paramURL, to: nm.stripped });
                            stripped = true;
                        } else {
                            deletes.push({ id: nm.id, reason: 'duplicate lonely nonce; strips to same key as another', paramURL: nm.paramURL });
                        }
                    });
                }
            }
        });
        print('    planned deletes: ' + deletes.length + ', planned in-place strips: ' + strips.length);
        deletes.forEach(function (d) {
            print('      DELETE _id=' + toJsonString(d.id) + '  (' + d.reason + ')');
            print('             paramURL=' + d.paramURL);
        });
        strips.forEach(function (s) {
            print('      STRIP  _id=' + toJsonString(s.id));
            print('             from=' + s.from);
            print('             to  =' + s.to);
        });
        let deleted = 0;
        let strippedCount = 0;
        let backedUp = 0;
        if (APPLY && (deletes.length > 0 || strips.length > 0)) {
            const backup = targetDb.getCollection(BACKUP_COLLECTION);
            // Back up every doc we are about to delete or modify, then act.
            deletes.forEach(function (d) {
                const original = coll.findOne({ _id: d.id });
                if (original !== null) {
                    original._migrationAction = 'delete';
                    original._migrationReason = d.reason;
                    original._migrationAt = new Date();
                    backup.insertOne(original);
                    backedUp++;
                    const res = coll.deleteOne({ _id: d.id });
                    deleted += res.deletedCount;
                }
            });
            strips.forEach(function (s) {
                const original = coll.findOne({ _id: s.id });
                if (original !== null) {
                    original._migrationAction = 'strip';
                    original._migrationFrom = s.from;
                    original._migrationTo = s.to;
                    original._migrationAt = new Date();
                    backup.insertOne(original);
                    backedUp++;
                    const res = coll.updateOne({ _id: s.id }, { $set: { [PARAM_URL]: s.to } });
                    strippedCount += res.modifiedCount;
                }
            });
            print('    APPLIED: backed up ' + backedUp + ', deleted ' + deleted + ', stripped ' + strippedCount
                + ' (backup collection: ' + BACKUP_COLLECTION + ')');
        } else if (deletes.length > 0 || strips.length > 0) {
            print('    DRY-RUN: no changes written. Re-run with APPLY=1 to execute.');
        } else {
            print('    nothing to do.');
        }
        return { db: targetDb.getName(), total: total, plannedDeletes: deletes.length, plannedStrips: strips.length,
            deleted: deleted, stripped: strippedCount, backedUp: backedUp };
    }

    print('=== Connectivity-params restore-queue nonce migration ===');
    print('Mode: ' + (APPLY ? 'APPLY (writes enabled)' : 'DRY-RUN (no writes; set APPLY=1 to execute)'));
    print('Connected to: ' + db.getMongo().toString());
    print('');
    // Auto-discover databases in this replica set / connection.
    const admin = db.getSiblingDB('admin');
    const dbList = admin.runCommand({ listDatabases: 1, nameOnly: true }).databases;
    const stats = [];
    dbList.forEach(function (entry) {
        const name = entry.name;
        if (name !== 'admin' && name !== 'local' && name !== 'config') {
            const targetDb = db.getSiblingDB(name);
            const collections = targetDb.getCollectionNames();
            if (collections.indexOf(COLLECTION) >= 0) {
                print('DB: ' + name);
                stats.push(migrateDatabase(targetDb));
                print('');
            }
        }
    });
    print('=== Fleet summary (this connection) ===');
    if (stats.length === 0) {
        print('  No database on this connection has a ' + COLLECTION + ' collection.');
    } else {
        let td = 0, ts = 0, tb = 0, tpd = 0, tps = 0;
        stats.forEach(function (s) {
            td += s.deleted; ts += s.stripped; tb += s.backedUp; tpd += s.plannedDeletes; tps += s.plannedStrips;
            print('  ' + s.db + ': planned del=' + s.plannedDeletes + ' strip=' + s.plannedStrips
                + (APPLY ? ('; applied del=' + s.deleted + ' strip=' + s.stripped + ' backedUp=' + s.backedUp) : ''));
        });
        print('  ---');
        print('  databases with collection : ' + stats.length);
        print('  total planned deletes     : ' + tpd);
        print('  total planned strips      : ' + tps);
        if (APPLY) {
            print('  total deleted             : ' + td);
            print('  total stripped            : ' + ts);
            print('  total backed up           : ' + tb);
        } else {
            print('  (dry run: nothing written)');
        }
    }
})();
