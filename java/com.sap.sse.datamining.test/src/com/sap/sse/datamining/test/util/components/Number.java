package com.sap.sse.datamining.test.util.components;

import java.io.Serializable;

import com.sap.sse.datamining.annotations.Dimension;

/*
 * DON'T CHANGE THE METHOD/CLASS NAMES!
 * The tests will fail, because they are reflected via constant strings.
 */

public class Number implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private int value;

    public Number(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    @Dimension(messageKey="length")
    public int getLength() {
        return String.valueOf(value).length();
    }
    
    @Dimension(messageKey="crossSum")
    public int getCrossSum() {
        int crossSum = 0;
        int value = this.value;
        while (value != 0) {
              crossSum += value % 10;
              value /= 10;
        }
        return crossSum;
    }

    @Override
    public String toString() {
        return "Number " + value;
    }

}
