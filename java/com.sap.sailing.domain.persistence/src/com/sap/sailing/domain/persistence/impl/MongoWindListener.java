package com.sap.sailing.domain.persistence.impl;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.tracking.TrackedRace;

public class MongoWindListener implements com.sap.sailing.domain.tracking.WindListener {
    private final TrackedRace trackedRace;
    private final String regattaName;
    private final WindSource windSource;
    private final MongoObjectFactoryImpl mongoObjectFactory;
    private final MongoCollection<org.bson.Document> windTracksCollection;

    public MongoWindListener(TrackedRace trackedRace, String regattaName, WindSource windSource,
            MongoObjectFactory mongoObjectFactory, MongoDatabase database) {
        super();
        this.regattaName = regattaName;
        this.trackedRace = trackedRace;
        this.windSource = windSource;
        this.mongoObjectFactory = (MongoObjectFactoryImpl) mongoObjectFactory;
        this.windTracksCollection = this.mongoObjectFactory.getWindTrackCollection();
    }

    @Override
    public void windDataReceived(Wind wind) {
        final Document windTrackEntry = createWindFixDocument(wind);
        windTracksCollection.withWriteConcern(WriteConcern.UNACKNOWLEDGED).insertOne(windTrackEntry);
    }

    private Document createWindFixDocument(Wind wind) {
        final Document windTrackEntry = mongoObjectFactory.storeWindTrackEntry(trackedRace.getRace(), regattaName, windSource, wind);
        return windTrackEntry;
    }

    @Override
    public void windDataReceived(Iterable<Wind> winds) {
        final List<Document> windTrackEntries = new ArrayList<>();
        for (final Wind wind : winds) {
            windTrackEntries.add(createWindFixDocument(wind));
        }
        windTracksCollection.withWriteConcern(WriteConcern.UNACKNOWLEDGED).insertMany(windTrackEntries);
    }

    @Override
    public void windDataRemoved(Wind wind) {
        Document windTrackEntry = mongoObjectFactory.storeWindTrackEntry(trackedRace.getRace(), regattaName, windSource, wind);
        windTracksCollection.deleteOne(windTrackEntry);
    }

    @Override
    public void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        // no need to change anything in MongoDB because MongoDB only keeps track of the fixes, not their averaging
    }

}
