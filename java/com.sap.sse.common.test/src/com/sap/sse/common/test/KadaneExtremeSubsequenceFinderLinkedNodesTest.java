package com.sap.sse.common.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinderLinkedNodesImpl;

public class KadaneExtremeSubsequenceFinderLinkedNodesTest extends KadaneExtremeSubsequenceFinderTest {
    @BeforeEach
    public void setUp() {
        finder = new KadaneExtremeSubsequenceFinderLinkedNodesImpl<>();
    }
}
