package com.sap.sse.datamining.impl.components.management;

import static com.sap.sse.datamining.test.util.ConcurrencyTestsUtil.sleepFor;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.management.DataMiningQueryManager;
import com.sap.sse.datamining.components.management.MemoryInfoProvider;
import com.sap.sse.datamining.components.management.MemoryMonitor;
import com.sap.sse.datamining.test.util.components.management.Test_MemoryMonitorAction;

public class TestQueryManagerMemoryMonitor {
    
    private Test_MemoryMonitorAction veryLow;
    private Test_MemoryMonitorAction low;
    private Test_MemoryMonitorAction medium;
    private Test_MemoryMonitorAction high1;
    private Test_MemoryMonitorAction high2;
    
    private Test_MemoryInfoProvider infoProvider;

    @BeforeEach
    public void initializeActionsAndInfoProvider() {
        veryLow = new Test_MemoryMonitorAction(0.9, 1l*1024*1024*1024);
        low = new Test_MemoryMonitorAction(0.8, 1l*1024*1024*1024);
        medium = new Test_MemoryMonitorAction(0.5, 1l*1024*1024*1024);
        high1 = new Test_MemoryMonitorAction(0.2, 1l*1024*1024*1024);
        high2 = new Test_MemoryMonitorAction(0.2, 1l*1024*1024*1024);
        infoProvider = new Test_MemoryInfoProvider(85 * 1024 * 1024, 100 * 1024 * 1024);
    }
    
    @Test
    public void testActionExecution() {
        Collection<Test_MemoryMonitorAction> actions = Arrays.asList(low, medium, high1);
        MemoryMonitor monitor = new QueryManagerMemoryMonitor(infoProvider, DataMiningQueryManager.NULL, actions, 40, TimeUnit.MILLISECONDS);
        sleepFor(50);
        actions.forEach(action -> assertThat(action.actionHasBeenPerformed(), is(false)));
        
        infoProvider.setFreeMemory(75 * 1024 * 1024);
        sleepFor(50);
        assertThat(low.actionHasBeenPerformed(), is(true));
        assertThat(medium.actionHasBeenPerformed(), is(false));
        assertThat(high1.actionHasBeenPerformed(), is(false));
        
        infoProvider.setFreeMemory(45 * 1024 * 1024);
        sleepFor(50);
        assertThat(low.actionHasBeenPerformed(), is(true));
        assertThat(medium.actionHasBeenPerformed(), is(true));
        assertThat(high1.actionHasBeenPerformed(), is(false));
        
        infoProvider.setFreeMemory(15 * 1024 * 1024);
        sleepFor(50);
        actions.forEach(action -> assertThat(action.actionHasBeenPerformed(), is(true)));

        monitor.pause(); // Pausing to suspend logging
    }
    
    @Test
    public void testActionExecutionWithEquallyImportantActions() {
        Collection<Test_MemoryMonitorAction> actions = Arrays.asList(low, medium, high1, high2);
        MemoryMonitor monitor = new QueryManagerMemoryMonitor(infoProvider, DataMiningQueryManager.NULL, actions, 40, TimeUnit.MILLISECONDS);
        sleepFor(50);
        actions.forEach(action -> assertThat(action.actionHasBeenPerformed(), is(false)));
        
        infoProvider.setFreeMemory(15 * 1024 * 1024);
        sleepFor(50);
        assertThat(low.actionHasBeenPerformed(), is(false));
        assertThat(medium.actionHasBeenPerformed(), is(false));
        assertThat(high1.actionHasBeenPerformed(), is(true));
        assertThat(high2.actionHasBeenPerformed(), is(true));

        monitor.pause(); // Pausing to suspend logging
    }

    @Test
    public void testPauseAndUnpause() {
        MemoryMonitor monitor = new QueryManagerMemoryMonitor(infoProvider, DataMiningQueryManager.NULL, Arrays.asList(veryLow), 40, TimeUnit.MILLISECONDS);
        sleepFor(50);
        assertThat(veryLow.actionHasBeenPerformed(), is(true));
        
        monitor.pause();
        sleepFor(50);
        veryLow.setActionHasBeenPerformed(false);
        sleepFor(75);
        assertThat(veryLow.actionHasBeenPerformed(), is(false));
        
        monitor.unpause();
        sleepFor(50);
        assertThat(veryLow.actionHasBeenPerformed(), is(true));

        monitor.pause(); // Pausing to suspend logging
    }
    
    private class Test_MemoryInfoProvider implements MemoryInfoProvider {

        private long freeMemory;
        private long totalMemory;

        public Test_MemoryInfoProvider(long freeMemory, long totalMemory) {
            this.freeMemory = freeMemory;
            this.totalMemory = totalMemory;
        }

        @Override
        public long freeMemory() {
            return freeMemory;
        }

        @Override
        public long totalMemory() {
            return totalMemory;
        }

        public void setFreeMemory(long freeMemory) {
            this.freeMemory = freeMemory;
        }

        @SuppressWarnings("unused")
        public void setTotalMemory(long totalMemory) {
            this.totalMemory = totalMemory;
        }
        
    }

}
