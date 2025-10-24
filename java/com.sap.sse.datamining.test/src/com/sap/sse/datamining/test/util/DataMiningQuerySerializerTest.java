package com.sap.sse.datamining.test.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.datamining.SailingPredefinedQueries;
import com.sap.sse.datamining.impl.DataMiningServerImpl;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.serialization.Base64SerializerDeserializer;

public class DataMiningQuerySerializerTest {
    @Test
    public void testSerializationAndDeserialization() {
        // test null
        testSerializationAndDeserialization(null);
        // test all predefined queries
        for (StatisticQueryDefinitionDTO query : new SailingPredefinedQueries().getQueries().values()) {
            testSerializationAndDeserialization(query);
        }
    }

    private void testSerializationAndDeserialization(final StatisticQueryDefinitionDTO dto) {
        String base64 = Base64SerializerDeserializer.toBase64(dto);
        StatisticQueryDefinitionDTO deserialized = new DataMiningServerImpl(
                /* executorService */ null, /* functionRegistry */ null, /* dataSourceProviderRegistry */ null,
                /* dataRetrieverChainDefinitionRegistry */ null, /* aggregationProcessorDefinitionRegistry */ null,
                /* queryDefinitionRegistry */ null).fromBase64String(base64);
        Assertions.assertEquals(dto, deserialized);
    }
}
