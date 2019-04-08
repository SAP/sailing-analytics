package com.sap.sailing.server.replication.test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.junit.Test;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sailing.server.gateway.jaxrs.spi.MasterDataResource;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.masterdata.MasterDataImporter;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;

import org.junit.Assert;

public class MediaReplicationTest extends AbstractServerReplicationTest {
      
    private void waitSomeTime() throws InterruptedException {
        Thread.sleep(1000); // wait for JMS to deliver the message and the message to be applied
    }
    
    /* util */
    private MediaTrack createMediaTrack() {
        String title = "title";
        String url = "url";
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = MillisecondsDurationImpl.ONE_HOUR;
        MimeType mimeType = MimeType.mp4;
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));
        MediaTrack mediaTrack = new MediaTrack(title, url, startTime, duration, mimeType, assignedRaces);
        return mediaTrack;
    }
    
    /* util */
    private MediaTrack cloneMediaTrack(MediaTrack mediaTrack) {
        MediaTrack clonedMediaTrack = new MediaTrack(mediaTrack.dbId, mediaTrack.title, mediaTrack.url, mediaTrack.startTime, mediaTrack.duration, mediaTrack.mimeType, mediaTrack.assignedRaces);
        return clonedMediaTrack;
    }
    
    @Test
    public void testBasicInitialLoad() throws Exception {
        assertNotSame(master, replica);
        assertEquals(Util.size(master.getAllMediaTracks()), Util.size(replica.getAllMediaTracks()));
    }
    
    @Test
    public void testAddMediaTrackReplication() throws InterruptedException {
        assertThat(Util.size(master.getAllMediaTracks()), is(0));
        assertThat(Util.size(replica.getAllMediaTracks()), is(0));
        MediaTrack mediaTrack = createMediaTrack();
        master.mediaTrackAdded(mediaTrack);
        assertThat(Util.size(master.getAllMediaTracks()), is(1));
        waitSomeTime();
        assertThat(Util.size(replica.getAllMediaTracks()), is(1));
        assertThat(replica.getAllMediaTracks().iterator().next().title, is(mediaTrack.title));
        assertThat(replica.getAllMediaTracks().iterator().next().url, is(mediaTrack.url));
        assertThat(replica.getAllMediaTracks().iterator().next().startTime, is(mediaTrack.startTime));
        assertThat(replica.getAllMediaTracks().iterator().next().duration, is(mediaTrack.duration));
        assertThat(replica.getAllMediaTracks().iterator().next().mimeType, is(mediaTrack.mimeType));
        assertThat(replica.getAllMediaTracks().iterator().next().assignedRaces.size(), is(1));
        assertThat(replica.getAllMediaTracks().iterator().next().assignedRaces, is(mediaTrack.assignedRaces));
    }

    @Test
    public void testDeleteMediaTrackReplication() throws InterruptedException {
        MediaTrack mediaTrack = createMediaTrack();
        master.mediaTrackAdded(mediaTrack);
        MediaTrack mediaTrackClone = cloneMediaTrack(mediaTrack);
        master.mediaTrackDeleted(mediaTrackClone);
        waitSomeTime();
        assertThat(Util.size(replica.getAllMediaTracks()), is(0));
    }

    @Test
    public void testUpdateMediaTrackTitleReplication() throws InterruptedException {
        MediaTrack mediaTrack = createMediaTrack();
        master.mediaTrackAdded(mediaTrack);
        MediaTrack mediaTrackClone = cloneMediaTrack(mediaTrack);
        mediaTrackClone.title = mediaTrack.title + "x";
        master.mediaTrackTitleChanged(mediaTrackClone);
        waitSomeTime();
        assertThat(Util.size(replica.getAllMediaTracks()), is(1));
        assertThat(replica.getAllMediaTracks().iterator().next().title, is(mediaTrack.title));
    }

    @Test
    public void testUpdateMediaTrackUrlReplication() throws InterruptedException {
        MediaTrack mediaTrack = createMediaTrack();
        master.mediaTrackAdded(mediaTrack);
        MediaTrack mediaTrackClone = cloneMediaTrack(mediaTrack);
        mediaTrackClone.url = mediaTrack.url + "x";
        master.mediaTrackUrlChanged(mediaTrackClone);
        waitSomeTime();
        assertThat(Util.size(replica.getAllMediaTracks()), is(1));
        assertThat(replica.getAllMediaTracks().iterator().next().url, is(mediaTrack.url));
    }

    @Test
    public void testUpdateMediaTrackStartTimeReplication() throws InterruptedException {
        MediaTrack mediaTrack = createMediaTrack();
        master.mediaTrackAdded(mediaTrack);
        MediaTrack mediaTrackClone = cloneMediaTrack(mediaTrack);
        mediaTrackClone.startTime = mediaTrack.startTime.plus(1000);
        master.mediaTrackStartTimeChanged(mediaTrackClone);
        waitSomeTime();
        assertThat(Util.size(replica.getAllMediaTracks()), is(1));
        assertThat(replica.getAllMediaTracks().iterator().next().startTime, is(mediaTrack.startTime));
    }

    @Test
    public void testUpdateMediaTrackDurationReplication() throws InterruptedException {
        MediaTrack mediaTrack = createMediaTrack();
        master.mediaTrackAdded(mediaTrack);
        MediaTrack mediaTrackClone = cloneMediaTrack(mediaTrack);
        mediaTrackClone.duration = mediaTrack.duration.plus(1000);
        master.mediaTrackDurationChanged(mediaTrackClone);
        waitSomeTime();
        assertThat(Util.size(replica.getAllMediaTracks()), is(1));
        assertThat(replica.getAllMediaTracks().iterator().next().duration, is(mediaTrack.duration));
    }
    
    @Test
    public void testUpdateMediaTrackAddRacesReplication() throws InterruptedException {
        MediaTrack mediaTrack = createMediaTrack();
        master.mediaTrackAdded(mediaTrack);
        MediaTrack mediaTrackClone = cloneMediaTrack(mediaTrack);
        mediaTrackClone.assignedRaces.add(new RegattaNameAndRaceName("505", "R1"));
        master.mediaTrackAssignedRacesChanged(mediaTrackClone);
        waitSomeTime();
        assertThat(Util.size(replica.getAllMediaTracks()), is(1));
        assertThat(replica.getAllMediaTracks().iterator().next().assignedRaces.size(), is(2));
        assertThat(replica.getAllMediaTracks().iterator().next().assignedRaces, is(mediaTrack.assignedRaces));
    }
    
    @Test
    public void testUpdateMediaTrackDeleteRacesReplication() throws InterruptedException {
        MediaTrack mediaTrack = createMediaTrack();
        master.mediaTrackAdded(mediaTrack);
        MediaTrack mediaTrackClone = cloneMediaTrack(mediaTrack);
        mediaTrackClone.assignedRaces.remove(new RegattaNameAndRaceName("49er", "R1"));
        master.mediaTrackAssignedRacesChanged(mediaTrackClone);
        waitSomeTime();
        assertThat(Util.size(replica.getAllMediaTracks()), is(1));
        assertThat(replica.getAllMediaTracks().iterator().next().assignedRaces.size(), is(0));
        assertThat(replica.getAllMediaTracks().iterator().next().assignedRaces, is(mediaTrack.assignedRaces));
    }

    @Test
    public void testMasterDataImportForMediaTracks() throws MalformedURLException, IOException, InterruptedException,
            ClassNotFoundException {
        // Setup source service
        RacingEventService sourceService = new RacingEventServiceImpl();
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        String regattaName1 = "49er";
        String regattaName2 = "49er FX";
        String missingRegattaName = "Missing Regatta";
        String raceName1 = "R1";
        String raceName2 = "R2";
        String raceName3 = "R3";
        String raceName4 = "R4";
        String raceName5 = "R5";
        String missingRaceName = "Missing Race";
        assignedRaces.add(new RegattaNameAndRaceName(regattaName1, raceName1));
        assignedRaces.add(new RegattaNameAndRaceName(regattaName1, raceName2));
        assignedRaces.add(new RegattaNameAndRaceName(regattaName1, raceName3));
        assignedRaces.add(new RegattaNameAndRaceName(regattaName2, raceName4));
        assignedRaces.add(new RegattaNameAndRaceName(regattaName2, raceName5));
        assignedRaces.add(new RegattaNameAndRaceName(missingRegattaName, missingRaceName));
        MediaTrack trackOnSource = new MediaTrack("testTitle", "http://test/test.mp4", new MillisecondsTimePoint(0),
                MillisecondsDurationImpl.ONE_HOUR, MimeType.mp4, assignedRaces);
        sourceService.mediaTrackAdded(trackOnSource);

        Collection<String> raceColumnNames = Arrays.asList(raceName1, raceName2, raceName3, raceName4, raceName5);
        Regatta regatta = TrackBasedTest.createTestRegatta(regattaName1, raceColumnNames);
        sourceService.addRegattaWithoutReplication(regatta);
        int[] discardThresholds = new int[0];
        RegattaLeaderboard leaderboard = sourceService.addRegattaLeaderboard(regatta.getRegattaIdentifier(),
                "leaderboard display name", discardThresholds);
        Collection<RaceIdentifier> raceIdentifiers = Arrays.asList(new RegattaNameAndRaceName(regattaName1, raceName1),
                new RegattaNameAndRaceName(regattaName1, raceName2),
                new RegattaNameAndRaceName(regattaName1, raceName3),
                new RegattaNameAndRaceName(regattaName2, raceName4),
                new RegattaNameAndRaceName(regattaName2, raceName5));
        TrackBasedTest.assignRacesToRegattaLeaderboardColumns(leaderboard, raceIdentifiers);
        boolean displayGroupsInReverseOrder = false;
        int[] overallLeaderboardDiscardThresholds = new int[0];
        UUID leaderboardGropuUuid = UUID.randomUUID();
        LeaderboardGroup leaderboardGroup = sourceService.addLeaderboardGroup(leaderboardGropuUuid,
                "leaderboard group name", "leaderboard group description", "leaderboard group display name",
                displayGroupsInReverseOrder, Collections.singletonList(leaderboard.getName()),
                overallLeaderboardDiscardThresholds, ScoringSchemeType.LOW_POINT);

        // Serialize
        List<String> groupNamesToExport = Collections.singletonList(leaderboardGroup.getName());

        final DomainFactory domainFactory;
        MasterDataResource resource = new MasterDataResource();
        MasterDataResource spyResource = spyResource(resource, sourceService);
        Response response = spyResource.getMasterDataByLeaderboardGroups(groupNamesToExport, false, true, false);
        StreamingOutput streamingOutput = (StreamingOutput) response.getEntity();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        UUID randomUUID = UUID.randomUUID();
        ByteArrayInputStream inputStream = null;
        try {
            streamingOutput.write(os);
            os.flush();
            // Delete all data above from the database, to allow recreating all of it on target server
            deleteAllDataFromDatabase();
            // Import in new service
            domainFactory = master.getBaseDomainFactory();
            // ensure that this class's class loader and with it the dependency to com.sap.sailing.domain.test
            // is known during de-serialization because anonymous inner classes from that bundle may be used
            // in the object graph, e.g., for RankingMetricConstructor objects based on locally-instantiated lambda
            // expressions
            master.addMasterDataClassLoader(this.getClass().getClassLoader());
            inputStream = new ByteArrayInputStream(os.toByteArray());
            MasterDataImporter importer = new MasterDataImporter(domainFactory, master);
            importer.importFromStream(inputStream, randomUUID, false);
        } finally {
            os.close();
            inputStream.close();
        }

        // ---Asserts---
        final Iterable<MediaTrack> targetTracksMaster = master.getAllMediaTracks();
        compareTracks(trackOnSource, targetTracksMaster);
        waitSomeTime();
        final Iterable<MediaTrack> targetTracksReplica = replica.getAllMediaTracks();
        compareTracks(trackOnSource, targetTracksReplica);
    }

    private void compareTracks(MediaTrack trackOnSource, Iterable<MediaTrack> targetTracksMaster) {
        Assert.assertEquals(1, Util.size(targetTracksMaster));
        MediaTrack trackOnTarget = targetTracksMaster.iterator().next();
        Assert.assertEquals(trackOnSource.dbId, trackOnTarget.dbId);
        Assert.assertEquals(trackOnSource.url, trackOnTarget.url);
        Assert.assertEquals(trackOnSource.assignedRaces, trackOnTarget.assignedRaces);
    }

    private <T extends AbstractSailingServerResource> T spyResource(T resource, RacingEventService service) {
        T spyResource = spy(resource);
        doReturn(service).when(spyResource).getService();
        return spyResource;
    }

    private void deleteAllDataFromDatabase() {
        MongoDBService service = MongoDBConfiguration.getDefaultTestConfiguration().getService();
        service.getDB().getWriteConcern().getJournal();
        service.getDB().drop();
    }

}
