package com.sap.sailing.domain.test;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.DynamicPerson;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.shared.dto.NamedDTO;

public abstract class StoredTrackBasedTest extends TrackBasedTest {
    private static final String RESOURCES = "resources/";

    protected DynamicGPSFixTrack<Competitor, GPSFixMoving> readTrack(Competitor competitor, Boat boat, String regattaName) throws FileNotFoundException, IOException {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> track = null;
        if (getFile(competitor, boat, regattaName).exists()) {
            ObjectInput oi = getInputStream(competitor, boat, regattaName);
            track = new DynamicGPSFixMovingTrackImpl<Competitor>(competitor, /* millisecondsOverWhichToAverage */
                    40000);
            try {
                GPSFixMoving fix;
                while ((fix = readGPSFixMoving(oi)) != null) {
                    track.addGPSFix(fix);
                }
            } catch (EOFException eof) {
                oi.close();
            }
        }
        return track;
    }
    
    ObjectInput getInputStream(Competitor competitor, Boat boat, String regattaName) throws FileNotFoundException, IOException {
        return new ObjectInputStream(new FileInputStream(getFile(competitor, boat, regattaName)));
    }

    ObjectOutput getOutputStream(Competitor competitor, Boat boat, String regattaName) throws FileNotFoundException, IOException {
        return new ObjectOutputStream(new FileOutputStream(getFile(competitor, boat, regattaName)));
    }

    private File getFile(Competitor competitor, Boat boat, String regattaName) {
        return new File(RESOURCES + regattaName + "-" + competitor.getName()
                + (boat.getSailID() == null ? "" : "-" + boat.getSailID()));
    }
    
    public static ObjectInputStream getObjectInputStream(String fileNameWithinResources) throws FileNotFoundException, IOException {
        return new CustomObjectInputStream(new FileInputStream(new File(RESOURCES+fileNameWithinResources)));
    }
    
    /**
     * We moved NamedDTO from sailing to sse some time ago. This breaks test cases that deserialize leaderboard DTO
     * data. This ensures that this data can correctly be deserialized without the need of manipulating the binary data.
     */
    private static class CustomObjectInputStream extends ObjectInputStream {
        public CustomObjectInputStream(InputStream in) throws IOException {
            super(in);
            enableResolveObject(true);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            final ObjectStreamClass osc = super.readClassDescriptor();
            if (osc.getName().equals("com.sap.sailing.domain.common.dto.NamedDTO")) {
                return ObjectStreamClass.lookup(NamedDTO.class);
            }
            return osc;
        }
    }
    
    private Set<String> getCompetitorNamesOfStoredTracks(String regattaName) {
        Set<String> result = new HashSet<String>();
        File d = new File(RESOURCES);
        final String separator = "-";
        for (String s : d.list()) {
            if (s.startsWith(regattaName+separator)) {
                result.add(s.substring(regattaName.length()+separator.length()));
            }
        }
        return result;
    }

    private void writeGPSFixMoving(GPSFixMoving fix, ObjectOutput oo) throws IOException {
        oo.writeLong(fix.getTimePoint().asMillis());
        oo.writeDouble(fix.getPosition().getLatDeg());
        oo.writeDouble(fix.getPosition().getLngDeg());
        oo.writeDouble(fix.getSpeed().getKnots());
        oo.writeDouble(fix.getSpeed().getBearing().getDegrees());
    }

    private GPSFixMoving readGPSFixMoving(ObjectInput oi) throws IOException {
        TimePoint timePoint = new MillisecondsTimePoint(oi.readLong());
        Position position = new DegreePosition(oi.readDouble(), oi.readDouble());
        SpeedWithBearing speedWithBearing = new KnotSpeedWithBearingImpl(oi.readDouble(), new DegreeBearingImpl(oi.readDouble()));
        return new GPSFixMovingImpl(position, timePoint, speedWithBearing, /* optionalTrueHeading */ null); // legacy fixes only; no need to worry about true heading
    }

    protected void storeTrack(Competitor competitor, Boat boat, DynamicGPSFixTrack<Competitor, GPSFixMoving> track, String regattaName)
            throws FileNotFoundException, IOException {
        ObjectOutput oo = getOutputStream(competitor, boat, regattaName);
        for (GPSFixMoving fix : track.getRawFixes()) {
            writeGPSFixMoving(fix, oo);
        }
        oo.close();
    }

    protected Map<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> loadTracks() throws FileNotFoundException, IOException {
        Map<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> tracks = new HashMap<Competitor, DynamicGPSFixTrack<Competitor,GPSFixMoving>>();
        final String KIELER_WOCHE = "Kieler Woche";
        for (String competitorName : getCompetitorNamesOfStoredTracks(KIELER_WOCHE)) {
            DynamicPerson p = new PersonImpl(competitorName, /* nationality */ null, /* dateOfBirth */ null, /* description */ null);
            DynamicTeam t = new TeamImpl(competitorName, Collections.singleton(p), /* coach */ null);
            Competitor c = new CompetitorImpl(competitorName, competitorName, "KYC", Color.RED, null, null, t, 
                    /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
            BoatClass boatClass = new BoatClassImpl("505", /* typicallyStartsUpwind */ true);
            Boat b = new BoatImpl("123", competitorName, boatClass, null);
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track = readTrack(c, b, KIELER_WOCHE);
            if (track != null) {
                tracks.put(c, track);
            }
        }
        return tracks;
    }

}
