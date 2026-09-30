package com.sap.sse.common.scalablevalue;

import java.io.Serializable;

public class ScalableDouble implements AbstractScalarValue<Double>, Serializable {
    private static final long serialVersionUID = -354261484569358609L;
    private final double value;
    
    public ScalableDouble(double value) {
        this.value = value;
    }
    
    @Override
    public ScalableDouble multiply(double factor) {
        return new ScalableDouble(factor*getValue());
    }

    @Override
    public ScalableDouble add(ScalableValue<Double, Double> t) {
        return new ScalableDouble(getValue()+t.getValue());
    }

    @Override
    public Double divide(double divisor) {
        return getValue()/divisor;
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public double getDistance(Double other) {
        return Math.abs(value-other);
    }

    @Override
    public String toString() {
        return Double.valueOf(value).toString();
    }

    @Override
    public int compareTo(Double o) {
        return Double.valueOf(value).compareTo(o);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(value);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScalableDouble other = (ScalableDouble) obj;
        if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
            return false;
        return true;
    }
}
