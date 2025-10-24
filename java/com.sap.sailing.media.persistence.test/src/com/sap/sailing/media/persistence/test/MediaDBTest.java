package com.sap.sailing.media.persistence.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.persistence.media.MediaDB;
import com.sap.sailing.domain.persistence.media.MediaDBFactory;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MimeType;

public class MediaDBTest extends AbstractMongoDBTest {

    public MediaDBTest() throws UnknownHostException, MongoException {
        super();
    }

    @Test
    public void testCreateVideoTrack() {
        final String videoTitle = "Test Video";
        final String url = "http://localhost:8888/media/HTML5/1809147112001_1842870496001_SAP-Regatta-Day02-Final_libtheora.ogv";
        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = MillisecondsDurationImpl.ONE_HOUR;
        MimeType mimeType = MimeType.ogv;
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));
        String dbId = mongoDB.insertMediaTrack(videoTitle, url, startTime, duration, mimeType, assignedRaces);
        
        assertNotNull(dbId);
        MediaTrack videoTrack = mongoDB.loadAllMediaTracks().iterator().next();
        assertNotNull(videoTrack);
        assertThat(videoTrack.dbId, Is.is(dbId));
        assertThat(videoTrack.title, Is.is(videoTitle));
    }
    
    @Test
    public void testCreateVideoTrackWithId() {
        final String dbId = new ObjectId().toHexString();
        final String videoTitle = "Test Video";
        final String url = "http://localhost:8888/media/HTML5/1809147112001_1842870496001_SAP-Regatta-Day02-Final_libtheora.ogv";
        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = MillisecondsDurationImpl.ONE_HOUR;
        MimeType mimeType = MimeType.ogv;
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));
        mongoDB.insertMediaTrackWithId(dbId, videoTitle, url, startTime, duration, mimeType, assignedRaces);
        MediaTrack videoTrack = mongoDB.loadAllMediaTracks().iterator().next();

        assertNotNull(videoTrack);
        assertThat(videoTrack.dbId, Is.is(dbId));
        assertThat(videoTrack.title, Is.is(videoTitle));
    }
    
    @Test
    public void testCreateVideoTrackWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            final String dbId = null;
            final String videoTitle = "Test Video";
            final String url = "http://localhost:8888/media/HTML5/1809147112001_1842870496001_SAP-Regatta-Day02-Final_libtheora.ogv";
            MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
            TimePoint startTime = MillisecondsTimePoint.now();
            Duration duration = MillisecondsDurationImpl.ONE_HOUR;
            MimeType mimeType = MimeType.ogv;
            Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
            assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));
            mongoDB.insertMediaTrackWithId(dbId, videoTitle, url, startTime, duration, mimeType, assignedRaces);
        });
    }
    
    @Test
    public void testCreateVideoTrackWithExistingId() {
        assertThrows(IllegalArgumentException.class, () -> {
            final String videoTitle = "Test Video";
            final String url = "http://localhost:8888/media/HTML5/1809147112001_1842870496001_SAP-Regatta-Day02-Final_libtheora.ogv";
            MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
            TimePoint startTime = MillisecondsTimePoint.now();
            Duration duration = MillisecondsDurationImpl.ONE_HOUR;
            MimeType mimeType = MimeType.ogv;
            Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
            assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));
            String dbId = mongoDB.insertMediaTrack(videoTitle, url, startTime, duration, mimeType, assignedRaces);
            mongoDB.insertMediaTrackWithId(dbId, videoTitle, url, startTime, duration, mimeType, assignedRaces);
        });
    }
    
//    @Test
//    public void testImportVideoTrackToEmptyDb() throws Exception {
//        //insert test object
//        final String dbId = new ObjectId().toHexString();
//        final String title = "Test Video";
//        final String url = "test";
//        Date date = new Date();
//        int duration = 23;
//        String mimeType = MimeType.ogv.name();
//        
//        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
//
//        mongoDB.importMediaTrack(dbId, title, url, date, duration, mimeType);
//        
//        List<DBMediaTrack> allMediaTracks = mongoDB.loadAllMediaTracks();
//        assertThat(allMediaTracks.size(), is(1));
//        DBMediaTrack dbMediaTrack = allMediaTracks.get(0);
//        assertThat(dbMediaTrack.dbId, is(dbId));
//        assertThat(dbMediaTrack.title, is(title));
//        assertThat(dbMediaTrack.url, is(url));
//        assertThat(dbMediaTrack.startTime, is(date));
//        assertThat(dbMediaTrack.duration, is(duration));
//        assertThat(dbMediaTrack.mimeType, is(mimeType));
//    }
//
//    @Test(expected=NullPointerException.class)
//    public void testImportVideoTrackNullId() throws Exception {
//        //insert test object
//        final String dbId = null;
//        final String title = "Test Video";
//        final String url = "test";
//        Date date = new Date();
//        int duration = 23;
//        String mimeType = MimeType.ogv.name();
//        
//        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
//
//        mongoDB.importMediaTrack(dbId, title, url, date, duration, mimeType);
//    }
//
//    @Test
//    public void testImportVideoTrackToExistingTrack_ChangedTitle() throws Exception {
//        //insert test object
//        final String title = "Test Video";
//        final String url = "test";
//        Date date = new Date();
//        int duration = 23;
//        String mimeType = MimeType.ogv.name();
//        
//        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
//        String dbId = mongoDB.insertMediaTrack(title, url, date, duration, mimeType);
//        
//        String newTitle = title + "x";
//        boolean trackCreated = mongoDB.importMediaTrack(dbId, newTitle, url, date, duration, mimeType);
//        
//        assertThat(trackCreated, equalTo(true));
//        List<DBMediaTrack> allMediaTracks = mongoDB.loadAllMediaTracks();
//        assertThat(allMediaTracks.size(), is(1));
//        DBMediaTrack dbMediaTrack = allMediaTracks.get(0);
//        assertThat(dbMediaTrack.dbId, is(dbId));
//        assertThat(dbMediaTrack.title, is(newTitle));
//        assertThat(dbMediaTrack.url, is(url));
//        assertThat(dbMediaTrack.startTime, is(date));
//        assertThat(dbMediaTrack.duration, is(duration));
//        assertThat(dbMediaTrack.mimeType, is(mimeType));
//    }
//    
//    @Test
//    public void testImportVideoTrackToExistingTrack_ChangedUrl() throws Exception {
//        //insert test object
//        final String title = "Test Video";
//        final String url = "test";
//        Date date = new Date();
//        int duration = 23;
//        String mimeType = MimeType.ogv.name();
//        
//        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
//        String dbId = mongoDB.insertMediaTrack(title, url, date, duration, mimeType);
//        
//        String newUrl = url + "x";
//        boolean trackCreated = mongoDB.importMediaTrack(dbId, title, newUrl, date, duration, mimeType);
//        
//        assertThat(trackCreated, equalTo(true));
//        List<DBMediaTrack> allMediaTracks = mongoDB.loadAllMediaTracks();
//        assertThat(allMediaTracks.size(), is(1));
//        DBMediaTrack dbMediaTrack = allMediaTracks.get(0);
//        assertThat(dbMediaTrack.dbId, is(dbId));
//        assertThat(dbMediaTrack.title, is(title));
//        assertThat(dbMediaTrack.url, is(newUrl));
//        assertThat(dbMediaTrack.startTime, is(date));
//        assertThat(dbMediaTrack.duration, is(duration));
//        assertThat(dbMediaTrack.mimeType, is(mimeType));
//    }
//    
//    @Test
//    public void testImportVideoTrackToExistingTrack_ChangedStartTime() throws Exception {
//        //insert test object
//        final String title = "Test Video";
//        final String url = "test";
//        Date startTime = new Date();
//        int duration = 23;
//        String mimeType = MimeType.ogv.name();
//        
//        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
//        String dbId = mongoDB.insertMediaTrack(title, url, startTime, duration, mimeType);
//        
//        Date newStartTime = new Date(startTime.getTime() + 1);
//        boolean trackCreated = mongoDB.importMediaTrack(dbId, title, url, newStartTime, duration, mimeType);
//        
//        assertThat(trackCreated, equalTo(true));
//        List<DBMediaTrack> allMediaTracks = mongoDB.loadAllMediaTracks();
//        assertThat(allMediaTracks.size(), is(1));
//        DBMediaTrack dbMediaTrack = allMediaTracks.get(0);
//        assertThat(dbMediaTrack.dbId, is(dbId));
//        assertThat(dbMediaTrack.title, is(title));
//        assertThat(dbMediaTrack.url, is(url));
//        assertThat(dbMediaTrack.startTime, is(newStartTime));
//        assertThat(dbMediaTrack.duration, is(duration));
//        assertThat(dbMediaTrack.mimeType, is(mimeType));
//    }
//    
//    @Test
//    public void testImportVideoTrackToExistingTrack_Changedduration() throws Exception {
//        //insert test object
//        final String title = "Test Video";
//        final String url = "test";
//        Date startTime = new Date();
//        int duration = 23;
//        String mimeType = MimeType.ogv.name();
//        
//        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
//        String dbId = mongoDB.insertMediaTrack(title, url, startTime, duration, mimeType);
//        
//        int newDurationInMillis = duration + 1;
//        boolean trackCreated = mongoDB.importMediaTrack(dbId, title, url, startTime, newDurationInMillis, mimeType);
//        
//        assertThat(trackCreated, equalTo(true));
//        List<DBMediaTrack> allMediaTracks = mongoDB.loadAllMediaTracks();
//        assertThat(allMediaTracks.size(), is(1));
//        DBMediaTrack dbMediaTrack = allMediaTracks.get(0);
//        assertThat(dbMediaTrack.dbId, is(dbId));
//        assertThat(dbMediaTrack.title, is(title));
//        assertThat(dbMediaTrack.url, is(url));
//        assertThat(dbMediaTrack.startTime, is(startTime));
//        assertThat(dbMediaTrack.duration, is(newDurationInMillis));
//        assertThat(dbMediaTrack.mimeType, is(mimeType));
//    }
//    
    @Test
    public void testLoadAllVideoTracks() {
        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
        int testEntryCount = 10;
        storeNumberOfTestMediaTracks(mongoDB, testEntryCount);
        List<MediaTrack> allMediaTracks = mongoDB.loadAllMediaTracks();
        assertThat(allMediaTracks.size(), is(testEntryCount));
    }

    private void storeNumberOfTestMediaTracks(MediaDB mongoDB, int count) {
        final String videoTitleTemplate = "Test Video ";
        final String url = "http://localhost:8888/media/HTML5/1809147112001_1842870496001_SAP-Regatta-Day02-Final_libtheora.ogv";
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = MillisecondsDurationImpl.ONE_HOUR;
        MimeType mimeType = MimeType.ogv;
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));


        for (int i = 0; i < count; i++) {
            mongoDB.insertMediaTrack(videoTitleTemplate + i, url, startTime, duration, mimeType, assignedRaces);
        }
    }

    @Test
    public void testDeleteVideoTrack() {
        //insert test object
        final String videoTitle = "Test Video";
        final String url = "test";
        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = MillisecondsDurationImpl.ONE_HOUR;
        MimeType mimeType = MimeType.ogv;
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));
        String dbId = mongoDB.insertMediaTrack(videoTitle, url, startTime, duration, mimeType, assignedRaces);
        
        //delete
        mongoDB.deleteMediaTrack(dbId);
        
        //assert not exists --> RuntimeException
        assertThat(mongoDB.loadAllMediaTracks().size(), is(0));
    }

    @Test
    public void testUpdateStartTime() {
        //insert test object
        final String videoTitle = "Test Video";
        final String url = "test";
        MediaDB mongoDB = MediaDBFactory.INSTANCE.getMediaDB(getMongoService());
        TimePoint originalDate = MillisecondsTimePoint.now();
        Duration duration = MillisecondsDurationImpl.ONE_HOUR;
        MimeType mimeType = MimeType.ogv;
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));
        String dbId = mongoDB.insertMediaTrack(videoTitle, url, originalDate, duration, mimeType, assignedRaces);

        
        //update with new date
        TimePoint newDate = originalDate.plus(1000);
        mongoDB.updateStartTime(dbId, newDate);
        
        //assert start time is updated and everything else preserved
        MediaTrack videoTrack = mongoDB.loadAllMediaTracks().iterator().next();
        assertThat(videoTrack.startTime, is(newDate));
        assertThat(videoTrack.duration, is(duration));
        assertThat(videoTrack.title, is(videoTitle));
        assertThat(videoTrack.url, is(url));
        assertThat(videoTrack.mimeType, is(mimeType));
        assertThat(videoTrack.assignedRaces, is(assignedRaces));
    }
    
}
