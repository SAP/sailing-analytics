package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.util.EnumMap;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.SortingOrder;

public class EnumMapSerializationTest {
    private static enum MyBoolean {
        TRUE, FALSE
    };

    private final EnumMap<MyBoolean, Integer> enumMap = new EnumMap<>(MyBoolean.class);

    @Test
    public void testEmptyEnumMap() throws IOException, ClassNotFoundException {
        assertSame(MyBoolean.class, com.sap.sse.util.EnumMapUtil.getKeyType(enumMap));
    }

    @Test
    public void testNonEmptyEnumMap() throws IOException, ClassNotFoundException {
        enumMap.put(MyBoolean.TRUE, 1);
        enumMap.put(MyBoolean.FALSE, 0);
        assertSame(MyBoolean.class, com.sap.sse.util.EnumMapUtil.getKeyType(enumMap));
    }
    
    @Test
    public void testEmptyEnumMapWithSortingOrderKeys() throws IOException, ClassNotFoundException {
        final EnumMap<SortingOrder, Integer> myEnumMap = new EnumMap<>(SortingOrder.class);
        assertSame(SortingOrder.class, com.sap.sse.util.EnumMapUtil.getKeyType(myEnumMap));
    }
}
