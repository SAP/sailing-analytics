package com.sap.sailing.landscape.impl;

import java.net.InetAddress;
import java.util.Map;

import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.procedures.SailingProcessConfigurationVariables;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.MapBuilder;
import com.sap.sse.landscape.application.ProcessFactory;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.impl.ApplicationProcessHostImpl;

public class SailingAnalyticsHostImpl<ShardingKey, HostT extends SailingAnalyticsHost<ShardingKey>>
extends ApplicationProcessHostImpl<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>, HostT>
implements SailingAnalyticsHost<ShardingKey> {
    public SailingAnalyticsHostImpl(String instanceId, AwsAvailabilityZone availabilityZone,
            InetAddress privateIpAddress, TimePoint launchTimePoint,
            AwsLandscape<ShardingKey> landscape, ProcessFactory<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>, HostT> processFactoryFromHostAndServerDirectory) {
        super(instanceId, availabilityZone, privateIpAddress, launchTimePoint, landscape, processFactoryFromHostAndServerDirectory);
    }

    @Override
    protected Map<String, Boolean> getAdditionalEnvironmentPropertiesAndWhetherStringTyped() {
        return MapBuilder.of(super.getAdditionalEnvironmentPropertiesAndWhetherStringTyped())
                .put(SailingProcessConfigurationVariables.EXPEDITION_PORT.name(), /* String-typed */ false)
                .put(SailingProcessConfigurationVariables.IGTIMI_RIOT_PORT.name(), /* String-typed */ false)
                .build();
    }

    /**
     * Shared sailing hosts are identified by checking their tag
     * {@link SharedLandscapeConstants#SAILING_ANALYTICS_APPLICATION_HOST_TAG} for value
     * {@link SharedLandscapeConstants#MULTI_PROCESS_INSTANCE_TAG_VALUE}.
     * <p>
     */
    @Override
    public boolean isSharedHost() {
        return getInstance().tags().stream()
                .anyMatch(tag -> tag.key().equals(SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG)
                        && tag.value().equals(SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_TAG_VALUE));
    }
}
