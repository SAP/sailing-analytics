package com.sap.sailing.server.trackfiles.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.common.trackfiles.TrackFilesDataSource;
import com.sap.sailing.domain.common.trackfiles.TrackFilesFormat;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.server.trackfiles.common.FormatNotSupportedException;
import com.sap.sailing.server.trackfiles.impl.ExportImpl;

public class TrackedRacesExportTest extends OnlineTracTracBasedTest {

    public TrackedRacesExportTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Override
    protected String getExpectedEventName() {
        return "49er European Championship";
    }

    @Before
    public void setUp() throws URISyntaxException, IOException, InterruptedException {
        URI storedUri = new URI("file:///"+new File("resources/event_20120905_erEuropean-Gold_fleet_-_race_1.mtb").getCanonicalPath());
        super.setUp(new URL("file:///"+new File("resources/event_20120905_erEuropean-Gold_fleet_-_race_1.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS, ReceiverType.MARKPASSINGS });
    }

    private byte[] getBytes(TrackFilesDataSource data, TrackFilesFormat format, TrackedRace race,
            boolean dataBeforeAfter, boolean rawFixes) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(out);
        ExportImpl.INSTANCE.writeAllData(Collections.singletonList(data), format, Collections.singletonList(race),
                true, true, zip);

        zip.flush();
        byte[] result = out.toByteArray();
        out.close();

        return result;
    }

    @Test
    public void doAllFormatsWork() throws IOException, FormatNotSupportedException {
        TrackedRace race = getTrackedRace();

        for (TrackFilesFormat format : TrackFilesFormat.values()) {
            byte[] data = getBytes(TrackFilesDataSource.COMPETITORS, format, race, true, true);

            assertTrue(data.length > 4000);
        }
    }

    @Test
    public void doAllDataSourcesWork() throws IOException, FormatNotSupportedException {
        TrackedRace race = getTrackedRace();

        for (TrackFilesDataSource source : TrackFilesDataSource.values()) {
            byte[] data = getBytes(source, TrackFilesFormat.Gpx11, race, true, true);

            assertTrue(data.length > 40);
        }
    }
}
