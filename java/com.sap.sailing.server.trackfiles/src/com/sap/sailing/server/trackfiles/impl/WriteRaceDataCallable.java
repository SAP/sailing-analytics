package com.sap.sailing.server.trackfiles.impl;

import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;

import com.sap.sailing.domain.common.trackfiles.TrackFilesDataSource;
import com.sap.sailing.domain.common.trackfiles.TrackFilesFormat;
import com.sap.sailing.domain.tracking.TrackedRace;

class WriteRaceDataCallable implements Callable<Void> {
    private final TrackedRace race;
    private final TrackFilesDataSource data;
    private final TrackFilesFormat format;
    private final boolean dataBeforeAfter;
    private final boolean rawFixes;
    private final WriteZipCallback callback;

    public WriteRaceDataCallable(TrackedRace race, TrackFilesDataSource data, TrackFilesFormat format,
            boolean dataBeforeAfter, boolean rawFixes, WriteZipCallback callback) {
        this.race = race;
        this.data = data;
        this.format = format;
        this.dataBeforeAfter = dataBeforeAfter;
        this.rawFixes = rawFixes;
        this.callback = callback;
    }

    // Can't put this into enum as parameter, as the enum resides in the
    // globally accessable J2SE 1.6 common bundle
    private DataRetriever getDataRetriever() {
        switch (data) {
        case BUOYS:
            return new BouyDataRetriever();
        case COMPETITORS:
            return new CompetitorDataRetriever();
        case WIND:
            return new WindDataRetriever();
        case MANEUVERS:
            return new ManueverDataRetriever();
        case METADATA:
            return new MetaDataRetriever();
        }
        return null;
    }

    public Void call() throws Exception {
        final DataRetriever dataRetriever = getDataRetriever();
        final byte[] result = dataRetriever.getData(format, race, dataBeforeAfter, rawFixes);
        final String suffix;
        if (data == TrackFilesDataSource.METADATA) {
            suffix = "txt";
        } else {
            suffix = format.suffix;
        }
        final ZipEntry entry = new ZipEntry(race.getRaceIdentifier().getRegattaName() + "/"
                + race.getRaceIdentifier().getRaceName() + " - " + data.toString() + "." + suffix);
        callback.write(entry, result);
        return null;
    }
}