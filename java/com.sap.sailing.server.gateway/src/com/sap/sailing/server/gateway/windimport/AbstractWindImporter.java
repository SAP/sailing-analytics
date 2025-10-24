package com.sap.sailing.server.gateway.windimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.fileupload.FileItem;
import org.apache.shiro.SecurityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.server.gateway.windimport.AbstractWindImporter.WindImportResult.RaceEntry;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.util.FileItemHelper;

public abstract class AbstractWindImporter {
    public static class UploadRequest {
        public String boatId;
        public final List<FileItem> files = new ArrayList<FileItem>();
        public List<RegattaAndRaceIdentifier> races = new ArrayList<RegattaAndRaceIdentifier>();;
    }

    public static class WindImportResult {
        private Date first;
        private Date last;
        public String error;

        public final List<RaceEntry> raceEntries = new ArrayList<RaceEntry>();

        public Date getFirst() {
            return first;
        }

        public Date getLast() {
            return last;
        }

        public void update(Wind newWind) {
            Date newDate = newWind.getTimePoint().asDate();
            if (this.first == null || newDate.before(this.first)) {
                this.first = newDate;
            }
            if (this.last == null || newDate.after(this.last)) {
                this.last = newDate;
            }
        }

        RaceEntry addRaceEntry(String regattaName, String raceName) {
            RaceEntry raceEntry = new RaceEntry(regattaName, raceName);
            raceEntries.add(raceEntry);
            return raceEntry;
        }

        static class RaceEntry {
            public final String regattaName;
            public final String raceName;
            private int count;
            private Date first;
            private Date last;

            private RaceEntry(String regattaName, String raceName) {
                this.regattaName = regattaName;
                this.raceName = raceName;
            }

            public void update(Wind newWind) {
                count++;
                Date newDate = newWind.getTimePoint().asDate();
                if (this.first == null || newDate.before(this.first)) {
                    this.first = newDate;
                }
                if (this.last == null || newDate.after(this.last)) {
                    this.last = newDate;
                }
            }

            public int getCount() {
                return count;
            }

            public Date getFirst() {
                return first;
            }

            public Date getLast() {
                return last;
            }

            private JSONObject json() {
                JSONObject result = new JSONObject();
                result.put("regattaName", regattaName);
                result.put("raceName", raceName);
                result.put("count", getCount());
                if (getFirst() != null) {
                    result.put("first", getFirst().getTime());
                }
                if (getLast() != null) {
                    result.put("last", getLast().getTime());
                }
                return result;
            }
        }

        public JSONObject json() {
            JSONObject result = new JSONObject();
            if (getFirst() != null) {
                result.put("first", getFirst().getTime());
            }
            if (getLast() != null) {
                result.put("last", getLast().getTime());
            }
            result.put("error", error);
            JSONArray raceEntriesJson = new JSONArray();
            for (RaceEntry raceEntry : raceEntries) {
                if (raceEntry.count > 0) {
                    raceEntriesJson.add(raceEntry.json());
                }
            }
            result.put("raceEntries", raceEntriesJson);
            return result;
        }
    }

    public void importWindForUploadRequest(RacingEventService service, WindImportResult windImportResult, UploadRequest uploadRequest)
            throws Exception {
        WindSource windSource = getDefaultWindSource(uploadRequest);
        List<DynamicTrackedRace> trackedRaces = new ArrayList<DynamicTrackedRace>();
        if (uploadRequest.races.size() > 0) {
            for (RegattaAndRaceIdentifier raceEntry : uploadRequest.races) {
                DynamicTrackedRace trackedRace = service.getTrackedRace(raceEntry);
                if (trackedRace != null) {
                    SecurityUtils.getSubject()
                        .checkPermission(trackedRace.getIdentifier().getStringPermission(DefaultActions.UPDATE));
                    trackedRaces.add(trackedRace);
                }
            }
        } else {
            for (Regatta regatta : service.getAllRegattas()) {
                for (RaceDefinition raceDefinition : regatta.getAllRaces()) {
                    final DynamicTrackedRace trackedRace = service.getTrackedRegatta(regatta).getExistingTrackedRace(raceDefinition);
                    if (trackedRace != null && SecurityUtils.getSubject()
                            .isPermitted(trackedRace.getIdentifier().getStringPermission(DefaultActions.UPDATE))) {
                        trackedRaces.add(trackedRace);
                    }
                }
            }
        }
        final Map<InputStream, Pair<String, Charset>> streamsWithFilenamesAndCharsets = new HashMap<>(); 
        for (FileItem file : uploadRequest.files) {
            streamsWithFilenamesAndCharsets.put(file.getInputStream(), new Pair<>(file.getName(), FileItemHelper.getCharset(file, Charset.forName("UTF-8"))));
        }
        importWindToWindSourceAndTrackedRaces(service, windImportResult, windSource, trackedRaces, streamsWithFilenamesAndCharsets);
    }

    /**
     * @param defaultWindSource
     *            the wind source to use for the wind fixes read using the {@link #importWind(WindSource, Map)} method for those
     *            fixes for which no explicit wind source has been returned by the {@link #importWind(WindSource, Map)} method.
     */
    public void importWindToWindSourceAndTrackedRaces(RacingEventService service, WindImportResult windImportResult, WindSource defaultWindSource,
            List<DynamicTrackedRace> trackedRaces, final Map<InputStream, Pair<String, Charset>> streamsWithFilenamesAndCharsets)
            throws Exception {
        Map<WindSource, Iterable<Wind>> windFixes = importWind(defaultWindSource, streamsWithFilenamesAndCharsets);
        for (final Entry<WindSource, Iterable<Wind>> windForSource : windFixes.entrySet()) {
            if (!Util.isEmpty(windForSource.getValue())) {
                for (DynamicTrackedRace trackedRace : trackedRaces) {
                    RegattaAndRaceIdentifier raceIdentifier = trackedRace.getRaceIdentifier();
                    RaceEntry raceEntry = windImportResult.addRaceEntry(raceIdentifier.getRegattaName(),
                            raceIdentifier.getRaceName());
                    for (Wind wind : windForSource.getValue()) {
                        windImportResult.update(wind);
                        if (trackedRace.recordWind(wind, windForSource.getKey() == null ? defaultWindSource : windForSource.getKey())) {
                            raceEntry.update(wind);
                        }
                    }
                    service.getPolarDataService().insertExistingFixes(trackedRace);
                }
            }
        }
    }

    /**
     * @param defaultWindSource
     *            the default wind source to use as the key of the map returned; implementations may, however, use this
     *            default wind source only as a copy template to produce finer-grained wind sources based on what the
     *            import stream contains
     * @param streamsWithFilenamesAndCharsets
     *            keys are the {@link InputStream}s from which to read wind data; values are pairs of which the first
     *            element is the filename, and the second is the character set to use for decoding the stream.
     *            The character sets must not be {@code null}. See {@link Charset#defaultCharset()}.
     * @return a map whose values are the wind fixes imported, keyed by the {@link WindSource} to which to add them.
     */
    protected abstract Map<WindSource, Iterable<Wind>> importWind(WindSource defaultWindSource,
            Map<InputStream, Pair<String, Charset>> streamsWithFilenamesAndCharsets)
            throws IOException, InterruptedException, FormatNotSupportedException, Exception;

    protected abstract WindSource getDefaultWindSource(UploadRequest uploadRequest);
}
