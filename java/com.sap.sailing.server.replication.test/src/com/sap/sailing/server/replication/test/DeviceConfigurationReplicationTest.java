package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.RegattaConfigurationImpl;
import com.sap.sse.common.Util;

public class DeviceConfigurationReplicationTest extends AbstractServerReplicationTest {

    @Test
    public void testCreateConfiguration() throws InterruptedException {
        DeviceConfigurationImpl configuration = new DeviceConfigurationImpl(new RegattaConfigurationImpl(), UUID.randomUUID(), "The Config");
        master.createOrUpdateDeviceConfiguration(configuration);
        Thread.sleep(1000);
        Iterable<DeviceConfiguration> configurations = replica.getAllDeviceConfigurations();
        assertEquals(1, Util.size(configurations));
    }
    
    @Test
    public void testUpdateConfiguration() throws InterruptedException {
        DeviceConfigurationImpl configuration = new DeviceConfigurationImpl(new RegattaConfigurationImpl(), UUID.randomUUID(), "23");
        configuration.setAllowedCourseAreaNames(Arrays.asList("hallo"));
        master.createOrUpdateDeviceConfiguration(configuration);
        configuration.setAllowedCourseAreaNames(Arrays.asList("hallo", "welt"));
        master.createOrUpdateDeviceConfiguration(configuration);
        Thread.sleep(1000);
        Iterable<DeviceConfiguration> configurations = replica.getAllDeviceConfigurations();
        assertEquals(1, Util.size(configurations));
        DeviceConfiguration replicatedConfiguration = configurations.iterator().next();
        assertEquals(configuration.getByNameCourseDesignerCourseNames(), replicatedConfiguration.getByNameCourseDesignerCourseNames());
    }
    
    @Test
    public void testRemoveConfiguration() throws InterruptedException {
        DeviceConfigurationImpl configuration = new DeviceConfigurationImpl(new RegattaConfigurationImpl(), UUID.randomUUID(), "24");
        master.createOrUpdateDeviceConfiguration(configuration);
        Thread.sleep(1000);
        master.removeDeviceConfiguration(configuration.getId());
        Thread.sleep(1000);
        Iterable<DeviceConfiguration> configurationMap = replica.getAllDeviceConfigurations();
        assertEquals(0, Util.size(configurationMap));
    }
}
