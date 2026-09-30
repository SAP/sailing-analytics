package com.sap.sailing.grib.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.grib.GribWindField;
import com.sap.sailing.grib.GribWindFieldFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.MappingIterable;
import com.sap.sse.util.LoggerAppender;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;

public class GribWindFieldFactoryImpl implements GribWindFieldFactory {
    private static final Logger logger = Logger.getLogger(GribWindFieldFactoryImpl.class.getName());
    private static final Level DEFAULT_ERROR_LOG_LEVEL = Level.INFO;
    
    /**
     * Weak references inserted into {@link #filesToCleanWhenGribWindFieldNoLongerUsed} must be registered with this
     * queue. When such a registration happens, a {@link #fileSystemCleaner} thread must be started while holding this
     * object's monitor (synchronized). The thread is then responsible for observing the queue and cleaning the
     * directories. When, while holding the monitor, the {@link #filesToCleanWhenGribWindFieldNoLongerUsed} map becomes
     * empty, the thread terminates and sets the {@link #fileSystemCleaner} field to {@code null} so that a new thread
     * must be created the next time a reference is entered into the map.
     */
    private final ReferenceQueue<GribWindField> referenceQueue = new ReferenceQueue<>();
    private Thread fileSystemCleaner;
    private final Map<WeakReference<GribWindField>, File> filesToCleanWhenGribWindFieldNoLongerUsed = new HashMap<>();

    private void removeDirectoryWhenWindFieldNoLongerStronglyReferenced(GribWindField windField, File directoryToRemove) {
        assert directoryToRemove.isDirectory();
        synchronized (this) {
            final boolean needToStartThread = filesToCleanWhenGribWindFieldNoLongerUsed.isEmpty();
            filesToCleanWhenGribWindFieldNoLongerUsed.put(new WeakReference<GribWindField>(windField, referenceQueue), directoryToRemove);
            if (needToStartThread && fileSystemCleaner == null) {
                fileSystemCleaner = new Thread(()->{
                    boolean finished = false;
                    while (!finished) {
                        try {
                            final Reference<? extends GribWindField> ref = referenceQueue.remove();
                            finished = cleanup(ref);
                        } catch (InterruptedException e) {
                            logger.log(Level.WARNING, "Interrupted while waiting for weak reference, giving up", e);
                            finished = true;
                        }
                    }
                }, "GRIB directory cleaner");
                fileSystemCleaner.setDaemon(true);
                fileSystemCleaner.start();
            }
        }
    }

    private boolean cleanup(final Reference<? extends GribWindField> ref) {
        boolean finished;
        final File dir;
        synchronized (GribWindFieldFactoryImpl.this) {
            dir = filesToCleanWhenGribWindFieldNoLongerUsed.remove(ref);
            finished = filesToCleanWhenGribWindFieldNoLongerUsed.isEmpty();
            if (finished) {
                fileSystemCleaner = null;
            }
        }
        rm_rf(dir);
        return finished;
    }
    
    @Override
    public synchronized void shutdown() {
        // clone key set because cleanup will remove the reference from filesToCleanWhenGribWindFieldNoLongerUsed
        // which would lead to a ConcurrentModificationException otherwise.
        for (final WeakReference<GribWindField> ref : new ArrayList<>(filesToCleanWhenGribWindFieldNoLongerUsed.keySet())) {
            cleanup(ref);
        }
    }

    /**
     * Removes directory and all its contents
     */
    private void rm_rf(File dir) {
        assert dir.isDirectory();
        for (final File f : dir.listFiles(f->true)) {
            if (f.isDirectory()) {
                rm_rf(f);
            } else {
                f.delete();
            }
        }
        dir.delete();
    }

    @Override
    public GribWindField createGribWindField(FeatureDataset... dataSets) {
        final GribWindField result;
        if (UVWindField.handles(dataSets)) {
            result = new UVWindField(dataSets);
        } else if (SpeedAndDirectionWindField.handles(dataSets)) {
            result = new SpeedAndDirectionWindField(dataSets);
        } else {
            throw new IllegalArgumentException("Couldn't find a wind field implementation handling data set(s) "+dataSets);
        }
        return result;
    }
    
    @Override
    public GribWindField createGribWindField(Iterable<String> locations) throws IOException {
        return createGribWindField(logger, DEFAULT_ERROR_LOG_LEVEL, locations);
    }
    
    @Override
    public GribWindField createGribWindField(Logger logger, Level level, Iterable<String> locations) throws IOException {
        final Formatter errorLog = createLogFormatter(logger, level);
        return createGribWindField(errorLog, locations);
    }

    @Override
    public GribWindField createGribWindField(Formatter errorLog, Iterable<String> locations) throws IOException {
        final List<FeatureDataset> dataSets = new ArrayList<>();
        for (final String location : locations) {
            FeatureDataset dataSet = FeatureDatasetFactoryManager.open(FeatureType.ANY, location, /* task */ null, errorLog);
            dataSets.add(dataSet);
        }
        return createGribWindField(dataSets.toArray(new FeatureDataset[0]));
    }

    @Override
    public GribWindField createGribWindFieldFromFiles(Iterable<File> files) throws IOException {
        return createGribWindFieldFromFiles(logger, DEFAULT_ERROR_LOG_LEVEL, files);
    }

    @Override
    public GribWindField createGribWindFieldFromFiles(Logger logger, Level level, Iterable<File> files)
            throws IOException {
        return createGribWindFieldFromFiles(createLogFormatter(logger, level), files);
    }

    @Override
    public GribWindField createGribWindFieldFromFiles(Formatter errorLog, Iterable<File> files) throws IOException {
        return createGribWindField(errorLog, new MappingIterable<>(files,
                f->{ try { return f.getCanonicalPath(); } catch (Exception e) { logger.log(Level.SEVERE, "Error obtaining GRIB file path", e); return null; } }));
    }

    @Override
    public GribWindField createGribWindFieldFromStreams(
            Map<InputStream, String> streamsAndFilenames) throws IOException {
        return createGribWindFieldFromStreams(logger, DEFAULT_ERROR_LOG_LEVEL, streamsAndFilenames);
    }

    @Override
    public GribWindField createGribWindFieldFromStreams(Logger logger, Level level,
            Map<InputStream, String> streamsAndFilenames) throws IOException {
        return createGribWindFieldFromStreams(createLogFormatter(logger, level), streamsAndFilenames);
    }

    @Override
    public GribWindField createGribWindFieldFromStreams(Formatter errorLog, Map<InputStream, String> streamsAndFilenames)
            throws IOException {
        final List<File> files = new ArrayList<>();
        for (final Entry<InputStream, String> e : streamsAndFilenames.entrySet()) {
            files.add(copyStreamToFile(e.getKey(), e.getValue()));
        }
        final GribWindField result = createGribWindFieldFromFiles(errorLog, files);
        for (final File file : files) {
            removeDirectoryWhenWindFieldNoLongerStronglyReferenced(result, file.getParentFile());
        }
        return result;
    }

    /**
     * Copies the contents of the input stream {@code s} into a temporary directory that is created solely for this
     * purpose. The {@code filename} will be used for the name of the file. The {@link File} object representing the
     * file written is returned so that it and its containing directory can be removed when the contents of the stream
     * are no longer needed.
     */
    private File copyStreamToFile(InputStream s, String filename) throws IOException {
        if (Util.hasLength(filename) && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
            throw new IllegalArgumentException("File extension must not contain '..' or a file separator like '/'.");
        }
        Path tempDir = Files.createTempDirectory("gribcache");
        Path filePath = tempDir.resolve(filename);
        Files.copy(s, filePath);
        return filePath.toFile();
    }

    @Override
    public Formatter createLogFormatter(Logger logger, Level level) {
        return new Formatter(new LoggerAppender(level, logger));
    }

}
