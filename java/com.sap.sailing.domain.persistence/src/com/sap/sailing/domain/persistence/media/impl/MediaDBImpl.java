package com.sap.sailing.domain.persistence.media.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteConcern;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.persistence.media.MediaDB;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MimeType;

/**
 * MongoDB Java API examples: http://blog.rasc.ch/?p=1096
 * 
 * @author D047974
 * 
 */
public class MediaDBImpl implements MediaDB {

    private static final int SORT_ASCENDING = 1;
    private static final int SORT_DESSCENDING = -1;
    // private static Logger logger = Logger.getLogger(MediaDBImpl.class.getName());
    private final DB database;
    private final BasicDBObject sortByStartTimeAndTitle;

    public MediaDBImpl(DB database) {
        super();
        this.database = database;
        sortByStartTimeAndTitle = new BasicDBObject();
        sortByStartTimeAndTitle.put(DbNames.Fields.STARTTIME.name(), SORT_DESSCENDING);
        sortByStartTimeAndTitle.put(DbNames.Fields.MEDIA_TITLE.name(), SORT_ASCENDING);
    }

    @Override
    public String insertMediaTrack(String title, String url, TimePoint startTime, Duration duration, MimeType mimeType,
            Set<RegattaAndRaceIdentifier> assignedRaces) {
        BasicDBObject dbMediaTrack = new BasicDBObject();
        dbMediaTrack.put(DbNames.Fields.MEDIA_TITLE.name(), title);
        dbMediaTrack.put(DbNames.Fields.MEDIA_URL.name(), url);
        dbMediaTrack.put(DbNames.Fields.STARTTIME.name(), startTime == null ? null : startTime.asDate());
        dbMediaTrack.put(DbNames.Fields.DURATION_IN_MILLIS.name(), duration == null ? null : duration.asMillis());
        dbMediaTrack.put(DbNames.Fields.MIME_TYPE.name(), mimeType == null ? null : mimeType.name());
        BasicDBList assignedRacesDb = new BasicDBList();
        if (assignedRaces != null) { //safety check to support deserialized instances imported from legacy servers  
            for (RegattaAndRaceIdentifier assignedRace : assignedRaces) {
                BasicDBObject object = new BasicDBObject();
                object.put(DbNames.Fields.REGATTA_NAME.name(), assignedRace.getRegattaName());
                object.put(DbNames.Fields.RACE_NAME.name(), assignedRace.getRaceName());
                assignedRacesDb.add(object);
            }
        }
        dbMediaTrack.put(DbNames.Fields.ASSIGNED_RACES.name(), assignedRacesDb);
        DBCollection dbVideos = getVideoCollection();
        dbVideos.insert(dbMediaTrack);
        return ((ObjectId) dbMediaTrack.get(DbNames.Fields._id.name())).toHexString();
    }

    @Override
    public void insertMediaTrackWithId(String dbId, String title, String url, TimePoint startTime, Duration duration,
            MimeType mimeType, Set<RegattaAndRaceIdentifier> assignedRaces) {
        BasicDBObject dbMediaTrack = new BasicDBObject();
        dbMediaTrack.put(DbNames.Fields._id.name(), new ObjectId(dbId));
        dbMediaTrack.put(DbNames.Fields.MEDIA_TITLE.name(), title);
        dbMediaTrack.put(DbNames.Fields.MEDIA_URL.name(), url);
        dbMediaTrack.put(DbNames.Fields.STARTTIME.name(), startTime == null ? null : startTime.asDate());
        dbMediaTrack.put(DbNames.Fields.DURATION_IN_MILLIS.name(), duration == null ? null : duration.asMillis());
        dbMediaTrack.put(DbNames.Fields.MIME_TYPE.name(), mimeType == null ? null : mimeType.name());
        BasicDBList assignedRacesDb = new BasicDBList();
        if (assignedRaces != null) { //safety check to support deserialized instances imported from legacy servers
            for (RegattaAndRaceIdentifier assignedRace : assignedRaces) {
                BasicDBObject object = new BasicDBObject();
                object.put(DbNames.Fields.REGATTA_NAME.name(), assignedRace.getRegattaName());
                object.put(DbNames.Fields.RACE_NAME.name(), assignedRace.getRaceName());
                assignedRacesDb.add(object);
            }
        }
        dbMediaTrack.put(DbNames.Fields.ASSIGNED_RACES.name(), assignedRacesDb);
        DBCollection dbVideos = getVideoCollection();
        try {
            dbVideos.insert(dbMediaTrack);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Duplicate key '" + dbId
                    + "' caused an error when importing media (title: '" + title + "')", e);
        }
    }

    private DBCollection getVideoCollection() {
        try {
            DBCollection dbVideos = database.getCollection(DbNames.Collections.VIDEOS.name());
            dbVideos.setWriteConcern(WriteConcern.FSYNC_SAFE);
            dbVideos.createIndex(new BasicDBObject(DbNames.Collections.VIDEOS.name(), 1));
            return dbVideos;
        } catch (NullPointerException e) {
            // sometimes, for reasons yet to be clarified, ensuring an index on the name field causes an NPE
            throw e;
        }
    }

    private MediaTrack createMediaTrackFromDb(DBObject dbObject) {
        String dbId = ((ObjectId) dbObject.get(DbNames.Fields._id.name())).toHexString();
        String title = (String) dbObject.get(DbNames.Fields.MEDIA_TITLE.name());
        String url = (String) dbObject.get(DbNames.Fields.MEDIA_URL.name());
        Date startTime = (Date) dbObject.get(DbNames.Fields.STARTTIME.name());
        Number duration = (Number) dbObject.get(DbNames.Fields.DURATION_IN_MILLIS.name());
        String mimeTypeText = (String) dbObject.get(DbNames.Fields.MIME_TYPE.name());
        MimeType mimeType = MimeType.byName(mimeTypeText);
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        BasicDBList assignedRacesDb = (BasicDBList) dbObject.get(DbNames.Fields.ASSIGNED_RACES.name());
        if (assignedRacesDb != null) { //safety check to support legacy instances
            for (Object assignedRace : assignedRacesDb) {
                BasicDBObject object = (BasicDBObject) assignedRace;
                String regattaName = (String) object.get(DbNames.Fields.REGATTA_NAME.name());
                String raceName = (String) object.get(DbNames.Fields.RACE_NAME.name());
                if (regattaName != null && raceName != null) {
                    assignedRaces.add(new RegattaNameAndRaceName(regattaName, raceName));
                }
            }
        }
        MediaTrack mediaTrack = new MediaTrack(dbId, title, url, startTime == null ? null : new MillisecondsTimePoint(
                startTime), duration == null ? null : new MillisecondsDurationImpl(duration.longValue()), mimeType,
                assignedRaces);
        return mediaTrack;
    }

    @Override
    public List<MediaTrack> loadAllMediaTracks() {
        DBCursor cursor = getVideoCollection().find().sort(sortByStartTimeAndTitle);
        List<MediaTrack> result = new ArrayList<>(cursor.count());
        while (cursor.hasNext()) {
            result.add(createMediaTrackFromDb(cursor.next()));
        }
        return result;
    }

    @Override
    public void deleteMediaTrack(String dbId) {
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put(DbNames.Fields._id.name(), new ObjectId(dbId));
        getVideoCollection().remove(dbObject);
    }

    @Override
    public void updateTitle(String dbId, String title) {
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.append(DbNames.Fields._id.name(), new ObjectId(dbId));

        BasicDBObject updateCommand = new BasicDBObject();
        updateCommand.append("$set", new BasicDBObject(DbNames.Fields.MEDIA_TITLE.name(), title));

        getVideoCollection().update(updateQuery, updateCommand);
    }

    @Override
    public void updateUrl(String dbId, String url) {
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.append(DbNames.Fields._id.name(), new ObjectId(dbId));

        BasicDBObject updateCommand = new BasicDBObject();
        updateCommand.append("$set", new BasicDBObject(DbNames.Fields.MEDIA_URL.name(), url));

        getVideoCollection().update(updateQuery, updateCommand);
    }

    @Override
    public void updateStartTime(String dbId, TimePoint startTime) {
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.append(DbNames.Fields._id.name(), new ObjectId(dbId));

        BasicDBObject updateCommand = new BasicDBObject();
        updateCommand.append("$set", new BasicDBObject(DbNames.Fields.STARTTIME.name(), startTime == null ? null
                : startTime.asDate()));

        getVideoCollection().update(updateQuery, updateCommand);
    }

    @Override
    public void updateDuration(String dbId, Duration duration) {
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.append(DbNames.Fields._id.name(), new ObjectId(dbId));

        BasicDBObject updateCommand = new BasicDBObject();
        updateCommand.append("$set", new BasicDBObject(DbNames.Fields.DURATION_IN_MILLIS.name(),
                duration == null ? null : duration.asMillis()));

        getVideoCollection().update(updateQuery, updateCommand);
    }

    @Override
    public void updateRace(String dbId, Set<RegattaAndRaceIdentifier> assignedRaces) {
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.append(DbNames.Fields._id.name(), new ObjectId(dbId));

        BasicDBList assignedRacesDb = new BasicDBList();
        if (assignedRaces != null) { //safety check to support deserialized instances imported from legacy servers
            for (RegattaAndRaceIdentifier assignedRace : assignedRaces) {
                BasicDBObject object = new BasicDBObject();
                object.put(DbNames.Fields.REGATTA_NAME.name(), assignedRace.getRegattaName());
                object.put(DbNames.Fields.RACE_NAME.name(), assignedRace.getRaceName());
                assignedRacesDb.add(object);
            }
        }
        BasicDBObject updateCommand = new BasicDBObject();
        updateCommand.append("$set", new BasicDBObject(DbNames.Fields.ASSIGNED_RACES.name(), assignedRacesDb));

        getVideoCollection().update(updateQuery, updateCommand);
    }

}
