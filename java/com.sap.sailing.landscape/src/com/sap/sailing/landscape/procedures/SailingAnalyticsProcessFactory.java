package com.sap.sailing.landscape.procedures;

import java.util.Map;
import java.util.function.Supplier;

import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.impl.SailingAnalyticsProcessImpl;
import com.sap.sse.landscape.application.ProcessFactory;
import com.sap.sse.landscape.aws.AwsLandscape;

public class SailingAnalyticsProcessFactory implements
        ProcessFactory<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, SailingAnalyticsHost<String>> {
    private final Supplier<AwsLandscape<String>> landscapeSupplier;
    
    public SailingAnalyticsProcessFactory(Supplier<AwsLandscape<String>> landscapeSupplier) {
        super();
        this.landscapeSupplier = landscapeSupplier;
    }

    @Override
    public SailingAnalyticsProcess<String> createProcess(SailingAnalyticsHost<String> host, int port,
            String serverDirectory, Integer telnetPort, String serverName, Map<String, Object> additionalProperties) {
        try {
            final Number expeditionUdpPort = (Number) additionalProperties
                    .get(SailingProcessConfigurationVariables.EXPEDITION_PORT.name());
            final Number igtimiRiotPort = (Number) additionalProperties
                    .get(SailingProcessConfigurationVariables.IGTIMI_RIOT_PORT.name());
            return new SailingAnalyticsProcessImpl<String>(port, host, serverDirectory, telnetPort, serverName,
                    expeditionUdpPort == null ? null : expeditionUdpPort.intValue(),
                    igtimiRiotPort == null ? null : igtimiRiotPort.intValue(), landscapeSupplier.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
