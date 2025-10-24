package com.sap.sailing.datamining.test.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import net.bytebuddy.agent.VirtualMachine;

/**
 * Requires com.sun.jna, com.sun.jna.platform bundles and the net.bytebuddy stuff to be on the classpath.
 * 
 * @author Axel Uhl (d043530)
 *
 */
@Disabled("This class is used only to demonstrate how injecting the JMX Prometheus Java agent works for a running VM")
public class TestAttachToVM {
    @Test
    public void testInjectAgentToRunningVM() throws IOException {
        final VirtualMachine vm = VirtualMachine.ForHotSpot.attach("520922");
        vm.loadAgent("/path/to/jmx_prometheus_javaagent-1.1.0.jar", "127.0.0.1:9000:/path/to/java/jmxPrometheusConfig.yaml");
        assertNotNull(vm);
        vm.detach();
    }
}
