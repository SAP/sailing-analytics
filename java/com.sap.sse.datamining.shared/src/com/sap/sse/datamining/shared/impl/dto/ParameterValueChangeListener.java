package com.sap.sse.datamining.shared.impl.dto;

import java.io.Serializable;

import com.sap.sse.datamining.shared.dto.DataMiningReportDTO;
import com.sap.sse.datamining.shared.dto.FilterDimensionParameter;
import com.sap.sse.datamining.shared.impl.dto.parameters.ParameterModelListener;

public class ParameterValueChangeListener implements ParameterModelListener, Serializable {
    private static final long serialVersionUID = 402977347893177440L;
    
    private ModifiableDataMiningReportDTO report;

    @Deprecated // for GWT serialization only
    ParameterValueChangeListener() {}
    
    public ParameterValueChangeListener(ModifiableDataMiningReportDTO report) {
        super();
        this.report = report;
    }

    @Override
    public void parameterAdded(DataMiningReportDTO report, FilterDimensionParameter parameter) {
    }

    @Override
    public void parameterRemoved(DataMiningReportDTO report, FilterDimensionParameter parameter) {
    }

    @Override
    public void parameterValueChanged(FilterDimensionParameter parameter, Iterable<? extends Serializable> oldValues) {
        report.adjustAllDimensionFiltersInQueriesUsingParameter(parameter);
    }
}