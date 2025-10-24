package com.sap.sailing.server.gateway.trackfiles.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.fileupload.FileItem;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sailing.domain.trackimport.GPSFixImporter.Callback;
import com.sap.sailing.server.gateway.trackfiles.impl.ImportResult.TrackImportDTO;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.FileItemHelper;

public class TrackFilesImporter {
    private static final Logger logger = Logger.getLogger(TrackFilesImporter.class.getName());
    
    private final RacingEventService service;
    private final TypeBasedServiceFinderFactory serviceFinderFactory;
    private final BundleContext context;

    public TrackFilesImporter(RacingEventService service, TypeBasedServiceFinderFactory serviceFinderFactory, BundleContext context) {
        this.service = service;
        this.serviceFinderFactory = serviceFinderFactory;
        this.context = context;
    }

    public void importFixes(ImportResult jsonResult, String prefImporterType, List<Pair<String, FileItem>> files)
            throws IOException {
        GPSFixImporter preferredImporter = null;
        if (prefImporterType != null && !prefImporterType.isEmpty()) {
            preferredImporter = serviceFinderFactory.createServiceFinder(GPSFixImporter.class)
                    .findService(prefImporterType);
        }
        importFilesWithPreferredImporter(files, jsonResult, preferredImporter);
    }

    void importFilesWithPreferredImporter(Iterable<Pair<String, FileItem>> files, ImportResult jsonResult,
            GPSFixImporter preferredImporter) throws IOException {
        for (Pair<String, FileItem> pair : files) {
            final String fileName = pair.getA();
            final FileItem fileItem = pair.getB();
            String fileExt = null;
            if (fileName.contains(".")) {
                fileExt = fileName.substring(fileName.lastIndexOf(".") + 1);
            }
            Set<GPSFixImporter> importersToTry = new LinkedHashSet<>();
            if (preferredImporter != null) {
                importersToTry.add(preferredImporter);
            }
            importersToTry.addAll(getGPSFixImporters(fileExt));
            importersToTry.addAll(getGPSFixImporters(null));
            logger.log(Level.INFO, "System knows " + importersToTry.size() + " importers: "
                    + importersToTry.stream().map(i -> i.getType()).collect(Collectors.joining(", ")));
            AtomicBoolean succeeded = new AtomicBoolean(false);
            parsersLoop: for (GPSFixImporter importer : importersToTry) {
                HashSet<TrackFileImportDeviceIdentifier> deviceIds = new HashSet<>();
                logger.log(Level.INFO, "Trying to import file " + fileName + " with importer " + importer.getType());
                try (BufferedInputStream in = new BufferedInputStream(fileItem.getInputStream())) {
                    try {
                        boolean ok = importer.importFixes(in, FileItemHelper.getCharset(fileItem), new Callback() {
                            @Override
                            public void addFix(GPSFix fix, TrackFileImportDeviceIdentifier device) {
                                storeFix(fix, device);
                                deviceIds.add(device);
                            }

                            @Override
                            public void addFixes(Iterable<GPSFix> fixes, TrackFileImportDeviceIdentifier device) {
                                storeFixes(fixes, device);
                                deviceIds.add(device);
                            }
                        }, true, fileName);
                        if (ok) {
                            succeeded.set(ok);
                        } else {
                            logger.log(Level.FINE,
                                    "Importer " + importer.getType() + " did not succesfully import fixes");
                        }
                    } catch (Exception e) {
                        logger.log(Level.INFO, "Failed with " + e.getClass().getSimpleName()
                                + " while importing file using " + importer.getType());
                        if (importer == preferredImporter) {
                            jsonResult.add(importer.getClass().getName(), fileName, e);
                        }
                    }
                }
                for (TrackFileImportDeviceIdentifier device : deviceIds) {
                    additionalDataExtractor(jsonResult, device);
                }
                if (succeeded.get()) {
                    logger.log(Level.INFO, "Successfully imported file " + fileName + " using " + importer.getType());
                    break parsersLoop;
                }
            }
            if (!succeeded.get()) {
                jsonResult.noImporterSucceeded(fileName);
            }
        }
    }

    protected void additionalDataExtractor(ImportResult jsonResult, TrackFileImportDeviceIdentifier device)
            throws TransformationException {
        TimeRange range = service.getSensorFixStore().getTimeRangeCoveredByFixes(device);
        long amount = service.getSensorFixStore().getNumberOfFixes(device);
        jsonResult.addTrackData(new TrackImportDTO(device.getId(), range, amount));
    }
    

    void storeFix(GPSFix fix, DeviceIdentifier deviceIdentifier) {
        try {
            service.getSensorFixStore().storeFix(deviceIdentifier, fix);
        } catch (NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not store fix for " + deviceIdentifier);
        }
    }

    <FixT extends Timed> void storeFixes(Iterable<FixT> fixes, DeviceIdentifier deviceIdentifier) {
        try {
            service.getSensorFixStore().storeFixes(deviceIdentifier, fixes, /* returnManeuverUpdate */ false, /* returnLiveDelay */ false);
        } catch (NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not store fix for " + deviceIdentifier);
        }
    }

    Collection<GPSFixImporter> getGPSFixImporters(String fileExtension) {
        List<GPSFixImporter> result = new ArrayList<>();
        Collection<ServiceReference<GPSFixImporter>> refs;
        try {
            Filter filter = null;
            if (fileExtension != null) {
                filter = context
                        .createFilter(String.format("(%s=%s)", GPSFixImporter.FILE_EXTENSION_PROPERTY, fileExtension));
            }
            refs = context.getServiceReferences(GPSFixImporter.class, filter == null ? null : filter.toString());
            for (ServiceReference<GPSFixImporter> ref : refs) {
                result.add(context.getService(ref));
            }
        } catch (InvalidSyntaxException e) {
            logger.log(Level.WARNING, "Could not create OSGi filter for file extension");
        }
        return result;
    }
}
