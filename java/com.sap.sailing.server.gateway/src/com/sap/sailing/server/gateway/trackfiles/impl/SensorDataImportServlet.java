package com.sap.sailing.server.gateway.trackfiles.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.server.gateway.impl.AbstractFileUploadServlet;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Import servlet for sensor data files. Importers are located through the OSGi service registry and mathed against the
 * name provided by the upload formular.
 */
public class SensorDataImportServlet extends AbstractFileUploadServlet {
    private static final long serialVersionUID = 1120226743039934620L;
    private static final Logger logger = Logger.getLogger(SensorDataImportServlet.class.getName());
    private static final int READ_BUFFER_SIZE = 1024 * 1024 * 1024;

    public void storeFixes(Iterable<DoubleVectorFix> fixes, DeviceIdentifier deviceIdentifier) {
        try {
            getService().getSensorFixStore().storeFixes(deviceIdentifier, fixes);
        } catch (NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not store fix for " + deviceIdentifier);
        }
    }

    /**
     * Searches the requested importer in the importers provided by the OSGi registry and imports the priovided sensor
     * data file.
     * 
     * @param files
     * @return
     * @throws IOException
     */
    private Iterable<TrackFileImportDeviceIdentifier> importFiles(Iterable<Pair<String, InputStream>> files)
            throws IOException {
        final Set<TrackFileImportDeviceIdentifier> deviceIds = new HashSet<>();
        final Map<DeviceIdentifier, TimePoint> from = new HashMap<>();
        final Map<DeviceIdentifier, TimePoint> to = new HashMap<>();
        final Collection<DoubleVectorFixImporter> availableImporters = new LinkedHashSet<>();
        availableImporters.addAll(getOSGiRegisteredImporters());
        for (Pair<String, InputStream> file : files) {
            final String requestedImporterName = file.getA();
            BufferedInputStream in = new BufferedInputStream(file.getB()) {
                @Override
                public void close() throws IOException {
                    // prevent importers from closing this stream
                }
            };
            DoubleVectorFixImporter importerToUse = null;
            for (DoubleVectorFixImporter candidate : availableImporters) {
                if (candidate.getType().equals(requestedImporterName)) {
                    importerToUse = candidate;
                    break;
                }
            }
            if (importerToUse == null) {
                throw new RuntimeException("Sensor importer not found");
            }
            in.mark(READ_BUFFER_SIZE);

            try {
                in.reset();
            } catch (IOException e1) {
                logger.log(Level.SEVERE, "Could not reset stream", e1);
            }
            logger.log(Level.INFO,
                    "Going to import sensor data file  with importer " + importerToUse.getClass().getSimpleName());
            try {
                importerToUse.importFixes(in, new DoubleVectorFixImporter.Callback() {
                    @Override
                    public void addFixes(Iterable<DoubleVectorFix> fixes, TrackFileImportDeviceIdentifier device) {
                        deviceIds.add(device);
                        storeFixes(fixes, device);
                        TimePoint earliestFixSoFarFromCurrentDevice = from.get(device);
                        TimePoint latestFixSoFarFromCurrentDevice = to.get(device);
                        for (DoubleVectorFix fix : fixes) {
                            if (earliestFixSoFarFromCurrentDevice == null
                                    || earliestFixSoFarFromCurrentDevice.after(fix.getTimePoint())) {
                                earliestFixSoFarFromCurrentDevice = fix.getTimePoint();
                                from.put(device, earliestFixSoFarFromCurrentDevice);
                            }
                            if (latestFixSoFarFromCurrentDevice == null
                                    || latestFixSoFarFromCurrentDevice.before(fix.getTimePoint())) {
                                latestFixSoFarFromCurrentDevice = fix.getTimePoint();
                                to.put(device, latestFixSoFarFromCurrentDevice);
                            }
                        }
                    }
                }, requestedImporterName);
                logger.log(Level.INFO, "Successfully imported file " + requestedImporterName);
            } catch (FormatNotSupportedException e) {
                logger.log(Level.INFO, "Failed to import file " + requestedImporterName);
            }
        }
        return deviceIds;
    }

    /**
     * Process the uploaded file items.
     */
    @Override
    protected void process(List<FileItem> fileItems, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        List<Pair<String, InputStream>> files = new ArrayList<>();
        String importerName = null;
        searchForPrefferedImporter: for (FileItem fi : fileItems) {
            if ("preferredImporter".equalsIgnoreCase(fi.getFieldName())) {
                importerName = fi.getString();
                break searchForPrefferedImporter;
            }
        }
        if (importerName == null) {
            importerName = "BRAVO";
        }
        for (FileItem fi : fileItems) {
            if ("file".equalsIgnoreCase(fi.getFieldName())) {
                files.add(new Pair<>(importerName, fi.getInputStream()));
            }
        }
        final Iterable<TrackFileImportDeviceIdentifier> mappingList = importFiles(files);
        resp.setContentType("text/html");
        for (TrackFileImportDeviceIdentifier mapping : mappingList) {
            String stringRep = mapping.getId().toString();
            resp.getWriter().println(stringRep);
        }
    }

    /**
     * Finds all {@link DoubleVectorFixImporter} service references in the OSGi context.
     * 
     * @return
     */
    private Collection<DoubleVectorFixImporter> getOSGiRegisteredImporters() {
        List<DoubleVectorFixImporter> result = new ArrayList<>();
        Collection<ServiceReference<DoubleVectorFixImporter>> refs;
        try {
            refs = getContext().getServiceReferences(DoubleVectorFixImporter.class, null);
            for (ServiceReference<DoubleVectorFixImporter> ref : refs) {
                result.add(getContext().getService(ref));
            }
        } catch (InvalidSyntaxException e) {
            logger.log(Level.WARNING, "Could not create OSGi filter");
        }
        return result;
    }
}
