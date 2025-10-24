package com.sap.sailing.domain.queclinkadapter.tracker;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.queclinkadapter.FRIReport;
import com.sap.sailing.domain.queclinkadapter.Message;
import com.sap.sailing.domain.queclinkadapter.MessageParser;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;
import com.sap.sailing.domain.queclinkadapter.PositionRelatedReport;
import com.sap.sailing.domain.queclinkadapter.impl.AbstractMessageVisitor;
import com.sap.sailing.domain.queclinkadapter.impl.PositionRelatedReportToGPSFixConverter;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.server.trackfiles.common.BaseGPSFixImporterImpl;

public class QueclinkGPSFixImporter extends BaseGPSFixImporterImpl {
    @Override
    public boolean importFixes(InputStream inputStream, Charset charset, Callback callback,
            boolean inferSpeedAndBearing, String sourceName) throws Exception {
        final Map<String, TrackFileImportDeviceIdentifier> deviceIdentifiersForImeis = new HashMap<>();
        final PositionRelatedReportToGPSFixConverter converter = new PositionRelatedReportToGPSFixConverter();
        final Reader reader = new InputStreamReader(inputStream, charset);
        final MessageParser parser = MessageParser.create();
        final MessageVisitor<Void> visitor = new AbstractMessageVisitor<Void>() {
            @Override
            public Void visit(FRIReport friReport) {
                final TrackFileImportDeviceIdentifier deviceIdentifier = deviceIdentifiersForImeis.computeIfAbsent(friReport.getImei(), imei->new TrackFileImportDeviceIdentifierImpl(sourceName, imei));
                for (final PositionRelatedReport prr : friReport.getPositionRelatedReports()) {
                    if (prr.getPosition() != null) {
                        addFixAndInfer(callback, inferSpeedAndBearing, converter.createGPSFixFromPositionRelatedReport(prr), deviceIdentifier);
                    }
                }
                return null;
            }
        };
        for (final Message m : parser.parse(reader)) {
            m.accept(visitor);
        }
        return true;
    }
    
    @Override
    public Iterable<String> getSupportedFileExtensions() {
        return Arrays.asList("qec", "", "log");
    }

    @Override
    public String getType() {
        return "Queclink GPS Fix Importer";
    }

}
