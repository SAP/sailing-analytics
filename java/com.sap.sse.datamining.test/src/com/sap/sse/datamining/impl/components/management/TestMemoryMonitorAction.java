package com.sap.sse.datamining.impl.components.management;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.management.MemoryMonitorAction;
import com.sap.sse.datamining.test.util.components.management.Test_MemoryMonitorAction;

public class TestMemoryMonitorAction {
    
    @Test
    public void testSimpleActionOrdering() {
        MemoryMonitorAction low = new Test_MemoryMonitorAction(0.8, /* 2GB freeMemoryInBytes */ 2l*1024*1024*1024);
        MemoryMonitorAction medium = new Test_MemoryMonitorAction(0.5, /* 2GB freeMemoryInBytes */ 2*1024*1024*1024);
        MemoryMonitorAction high1 = new Test_MemoryMonitorAction(0.2, /* 2GB freeMemoryInBytes */ 2*1024*1024*1024);
        MemoryMonitorAction high2 = new Test_MemoryMonitorAction(0.2, /* 2GB freeMemoryInBytes */ 2*1024*1024*1024);
        List<MemoryMonitorAction> actions = Arrays.asList(low, high2, high1, medium);
        Collections.sort(actions);

        List<MemoryMonitorAction> expectedOrder1 = Arrays.asList(high1, high2, medium, low);
        List<MemoryMonitorAction> expectedOrder2 = Arrays.asList(high2, high1, medium, low);
        assertTrue(actions.equals(expectedOrder1) || actions.equals(expectedOrder2));
    }
    
    @Test
    public void testActionPerforming() {
        Test_MemoryMonitorAction action = new Test_MemoryMonitorAction(0.2, /* 2GB freeMemoryInBytes */ 2l*1024*1024*1024);
        // enough ratio and absolute
        assertThat(action.checkMemoryAndPerformAction(0.3, /* 2GB freeMemoryInBytes */ 2l*1024*1024*1024+1), is(false));
        assertThat(action.actionHasBeenPerformed(), is(false));
        // enough ratio but too little absolute still won't fire
        assertThat(action.checkMemoryAndPerformAction(0.3, /* 2GB freeMemoryInBytes */ 2l*1024*1024*1024-1), is(false));
        assertThat(action.actionHasBeenPerformed(), is(false));
        // exactly matching ratio, and too little absolute won't fire
        assertThat(action.checkMemoryAndPerformAction(0.2, /* 2GB freeMemoryInBytes */ 2l*1024*1024*1024-1), is(false));
        assertThat(action.actionHasBeenPerformed(), is(false));
        // too little ratio and too little absolute will fire
        assertThat(action.checkMemoryAndPerformAction(0.1, /* 2GB freeMemoryInBytes */ 2l*1024*1024*1024-1), is(true));
        assertThat(action.actionHasBeenPerformed(), is(true));
    }

}
