package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class VariableArityParameterNeverNullTest {
    @Test
    public void callVariableArityWithoutActualValue() {
        m();
    }
    
    private void m(String... variableArityFormalParam) {
        assertNotNull(variableArityFormalParam);
    }
}
