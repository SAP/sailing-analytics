package com.sap.sailing.server.trackfiles.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sap.sailing.domain.common.trackfiles.TrackFilesDataSource;
import com.sap.sailing.domain.common.trackfiles.TrackFilesFormat;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.trackfiles.TrackFileExporter;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Export data to well-known formats such as GPX, KML etc. Internally, the RouteConverter Library is used.
 * 
 * Should you need to support another format, add this to {@link TrackFilesFormat} and extend the writeFixes method.
 * 
 * @author Fredrik Teschke
 * 
 */
public class TrackFileExporterImpl implements TrackFileExporter {
    private static final Logger log = Logger.getLogger(TrackFileExporterImpl.class.toString());

    /**
     * Writes the wanted data of all the races to the output stream. One file per race and data type.
     * 
     * @param dataBeforeAfter
     *            false: do not include data from before the race started and after the race ended
     */
    @Override
    public void writeAllData(List<TrackFilesDataSource> data, TrackFilesFormat format, List<TrackedRace> races,
            boolean dataBeforeAfter, boolean rawFixes, final ZipOutputStream out) throws IOException {
        WriteZipCallback callback = new WriteZipCallback() {
            @Override
            public synchronized void write(ZipEntry entry, byte[] data) throws IOException {
                out.putNextEntry(entry);
                out.write(data);
                out.closeEntry();
            }
        };
        List<WriteRaceDataCallable> callables = new ArrayList<>();
        ExecutorService executor = ThreadPoolUtil.INSTANCE.getDefaultForegroundTaskThreadPoolExecutor();
        List<String> errors = new ArrayList<>();
        for (TrackedRace race : races) {
            for (TrackFilesDataSource d : data) {
                WriteRaceDataCallable callable = new WriteRaceDataCallable(race, d, format, dataBeforeAfter, rawFixes,
                        callback);
                callables.add(callable);
            }
        }
        ThreadPoolUtil.INSTANCE.invokeAllAndLogExceptions(executor, Level.WARNING, "Error exporting race: %s", callables);
        if (errors.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String error : errors) {
                sb.append(error);
                sb.append("\n");
            }
            try {
                callback.write(new ZipEntry("ERRORS"), sb.toString().getBytes());
            } catch (Exception e) {
                log.log(Level.WARNING, "Error exporting race: " + e.getMessage());
            }
        }
    }
}
