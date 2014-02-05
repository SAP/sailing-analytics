package com.sap.sailing.datamining.impl.function;

import com.sap.sailing.datamining.ConcurrentFilterCriteria;
import com.sap.sailing.datamining.function.Function;
import com.sap.sse.datamining.shared.dto.FunctionDTO;

public class FunctionDTOFilterCriteria implements ConcurrentFilterCriteria<Function<?>> {

    private FunctionDTO functionDTOToMatch;

    public FunctionDTOFilterCriteria(FunctionDTO functionDTOToMatch) {
        this.functionDTOToMatch = functionDTOToMatch;
    }

    @Override
    public boolean matches(Function<?> function) {
        FunctionDTO functionDTO = function.asDTO();
        return functionDTOToMatch.equals(functionDTO);
    }

}
