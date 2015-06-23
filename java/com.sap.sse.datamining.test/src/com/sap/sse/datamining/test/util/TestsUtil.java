package com.sap.sse.datamining.test.util;

import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.components.management.AggregationProcessorDefinitionRegistry;
import com.sap.sse.datamining.components.management.DataRetrieverChainDefinitionRegistry;
import com.sap.sse.datamining.factories.DataMiningDTOFactory;
import com.sap.sse.datamining.impl.DataMiningServerImpl;
import com.sap.sse.datamining.impl.components.management.AggregationProcessorDefinitionManager;
import com.sap.sse.datamining.impl.components.management.DataRetrieverChainDefinitionManager;
import com.sap.sse.datamining.impl.functions.FunctionManager;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.CompoundResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;

public class TestsUtil {
    
    private static final String TEST_STRING_MESSAGES_BASE_NAME = "stringmessages/Test_StringMessages";
    private static ResourceBundleStringMessages TEST_STRING_MESSAGES;

    private static final String PRODUCTIVE_STRING_MESSAGES_BASE_NAME = "stringmessages/StringMessages";
    private static CompoundResourceBundleStringMessages EXTENDED_STRING_MESSAGES;
    
    private static final DataMiningDTOFactory dtoFactory = new DataMiningDTOFactory();
    
    public static DataMiningDTOFactory getDTOFactory() {
        return dtoFactory;
    }
    
    public static ResourceBundleStringMessages getTestStringMessages() {
        if (TEST_STRING_MESSAGES == null) {
            TEST_STRING_MESSAGES = new ResourceBundleStringMessagesImpl(TEST_STRING_MESSAGES_BASE_NAME, TestsUtil.class.getClassLoader());
        }
        
        return TEST_STRING_MESSAGES;
    }
    
    public static ResourceBundleStringMessages getTestStringMessagesWithProductiveMessages() {
        if (EXTENDED_STRING_MESSAGES == null) {
            EXTENDED_STRING_MESSAGES = new CompoundResourceBundleStringMessages();
            EXTENDED_STRING_MESSAGES.addStringMessages(getTestStringMessages());
            EXTENDED_STRING_MESSAGES.addStringMessages(new ResourceBundleStringMessagesImpl(PRODUCTIVE_STRING_MESSAGES_BASE_NAME, TestsUtil.class.getClassLoader()));
        }
        
        return EXTENDED_STRING_MESSAGES;
    }
    
    public static ModifiableDataMiningServer createNewServer() {
        FunctionManager functionManager = new FunctionManager();
        DataRetrieverChainDefinitionRegistry dataRetrieverChainDefinitionManager = new DataRetrieverChainDefinitionManager();
        AggregationProcessorDefinitionRegistry aggregationProcessorDefinitionManager = new AggregationProcessorDefinitionManager();
        return new DataMiningServerImpl(ConcurrencyTestsUtil.getExecutor(), functionManager, dataRetrieverChainDefinitionManager, aggregationProcessorDefinitionManager);
    }
    
    protected TestsUtil() { }

}
