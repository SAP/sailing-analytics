package com.sap.sse.datamining.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.DataSourceProvider;
import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.test.domain.Test_Regatta;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;

public class TestDataMiningServer {

    @Test
    public void testDataSourceProviderManagement() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        DataSourceProvider<Test_Regatta> dataSourceProvider = new AbstractDataSourceProvider<Test_Regatta>(Test_Regatta.class) {
            @Override
            public Test_Regatta getDataSource() {
                return null;
            }
        };
        
        Date beforeChange = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerDataSourceProvider(dataSourceProvider);
        assertThat(server.getComponentsChangedTimepoint().after(beforeChange), is(true));
        
        beforeChange = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerDataSourceProvider(dataSourceProvider);
        assertThat(server.getComponentsChangedTimepoint().after(beforeChange), is(true));
        
        beforeChange = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterDataSourceProvider(dataSourceProvider);
        assertThat(server.getComponentsChangedTimepoint().after(beforeChange), is(true));
        
        beforeChange = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterDataSourceProvider(dataSourceProvider);
        assertThat(server.getComponentsChangedTimepoint().after(beforeChange), is(false));
    }

}
