package com.sap.sailing.domain.common.scalablevalue;

public interface ComparableScalableValueWithDistance<ValueType, AveragesTo extends Comparable<AveragesTo>>
    extends ScalableValueWithDistance<ValueType, AveragesTo>, Comparable<AveragesTo> {
}
