package com.sap.sailing.landscape.procedures;

import java.net.InetAddress;

import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.impl.SailingAnalyticsHostImpl;
import com.sap.sailing.landscape.impl.SailingAnalyticsProcessImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.HostSupplier;

public class SailingAnalyticsHostSupplier<ShardingKey> implements HostSupplier<ShardingKey, SailingAnalyticsHost<ShardingKey>> {
    @Override
    public SailingAnalyticsHost<ShardingKey> supply(String instanceId, AwsAvailabilityZone availabilityZone, InetAddress privateIpAddress,
            TimePoint launchTimePoint, AwsLandscape<ShardingKey> landscape) {
        return new SailingAnalyticsHostImpl<>(instanceId, availabilityZone, privateIpAddress,
                launchTimePoint, landscape, (host, port, serverDirectory, telnetPort, serverName, additionalProperties)->{
                    try {
                        final Number expeditionPort = (Number) additionalProperties.get(SailingProcessConfigurationVariables.EXPEDITION_PORT.name());
                        final Number igtimiRiotPort = (Number) additionalProperties.get(SailingProcessConfigurationVariables.IGTIMI_RIOT_PORT.name());
                        return new SailingAnalyticsProcessImpl<ShardingKey>(port, host, serverDirectory, telnetPort, serverName,
                                expeditionPort==null?null:expeditionPort.intValue(),
                                igtimiRiotPort==null?null:igtimiRiotPort.intValue(), landscape);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
