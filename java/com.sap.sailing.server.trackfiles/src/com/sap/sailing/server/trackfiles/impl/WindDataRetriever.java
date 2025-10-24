package com.sap.sailing.server.trackfiles.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.trackfiles.TrackFilesFormat;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.tracking.TrackedRace;

import slash.navigation.gpx.GpxRoute;

public class WindDataRetriever extends AbstractDataRetriever {

    @Override
    public Collection<GpxRoute> getRoutes(TrackFilesFormat format, final TrackedRace race, boolean dataBeforeAfter,
            boolean rawFixes) throws FormatNotSupportedException, IOException {
        TrackReaderRetriever<WindSource, Wind> retriever = new TrackReaderRetriever<WindSource, Wind>() {
            @Override
            public TrackReader<WindSource, Wind> retrieveTrackReader(WindSource e) {
                return new TrackReaderImpl<WindSource, Wind>(race.getOrCreateWindTrack(e));
            }
        };
        final Set<WindSource> windSources = new HashSet<>();
        windSources.addAll(race.getWindSources(WindSourceType.EXPEDITION));
        windSources.addAll(race.getWindSources(WindSourceType.RACECOMMITTEE));
        windSources.addAll(race.getWindSources(WindSourceType.WEB));
        windSources.addAll(race.getWindSources(WindSourceType.WINDFINDER));
        windSources.addAll(race.getWindSources(WindSourceType.COMBINED));
        return getRoutes(race, dataBeforeAfter, rawFixes, WindToGpxPosition.INSTANCE, windSources,
                new NameReader<WindSource>() {
                    @Override
                    public String getName(WindSource s) {
                        return s.getTypeAndId();
                    }
                }, retriever);
    }

}
