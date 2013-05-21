package com.sap.sailing.gwt.ui.client.shared.filter;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.sap.sailing.domain.common.filter.BinaryOperator;
import com.sap.sailing.domain.common.filter.NumberFilter;
import com.sap.sailing.domain.common.filter.TextFilter;
import com.sap.sailing.domain.common.filter.TextOperator;
import com.sap.sailing.domain.common.filter.ValueFilter;
import com.sap.sailing.gwt.ui.shared.CompetitorDTO;

public class ValueFilterJsonDeSerializerUtil {
    public static final String FIELD_FILTER_NAME = "name";
    public static final String FIELD_FILTER_OPERATOR = "operator";
    public static final String FIELD_FILTER_VALUE = "value";
    public static final String FIELD_FILTER_TYPE = "type";
    
    public static final String VALUE_FILTER_TYPE = "ValueFilter";

    public static JSONObject serialize(ValueFilter<?, ?> filter) {
        JSONObject filterAsJsonObject = new JSONObject();

        filterAsJsonObject.put(FIELD_FILTER_NAME, new JSONString(filter.getName()));
        filterAsJsonObject.put(FIELD_FILTER_OPERATOR, new JSONString(filter.getOperator().getName()));
        filterAsJsonObject.put(FIELD_FILTER_VALUE, new JSONString(filter.getValue().toString()));
        filterAsJsonObject.put(FIELD_FILTER_TYPE, new JSONString(VALUE_FILTER_TYPE));

        return filterAsJsonObject;
    }

    public static FilterWithUI<CompetitorDTO> deserialize(JSONObject filterAsJsonObject) {
        JSONString filterNameValue = (JSONString) filterAsJsonObject.get(FIELD_FILTER_NAME);
        JSONString filterOperatorValue = (JSONString) filterAsJsonObject.get(FIELD_FILTER_OPERATOR);
        JSONString filterValueValue = (JSONString) filterAsJsonObject.get(FIELD_FILTER_VALUE);

        String filterName = filterNameValue.stringValue();
        String operator = filterOperatorValue.stringValue();
        String value = filterValueValue.stringValue();
        
        FilterWithUI<CompetitorDTO> filter = CompetitorFilterWithUIFactory.createFilter(filterName);
        if(filter != null && operator != null && value != null) {
            if(filter instanceof NumberFilter<?,?>) {
                @SuppressWarnings("unchecked")
                NumberFilter<CompetitorDTO, Integer> numberFilter = (NumberFilter<CompetitorDTO, Integer>) filter;
                numberFilter.setOperator(new BinaryOperator<Integer>(BinaryOperator.Operators.valueOf(operator)));
                numberFilter.setValue(Integer.valueOf(value));
            } else if(filter instanceof TextFilter<?>) {
                @SuppressWarnings("unchecked")
                TextFilter<CompetitorDTO> textFilter = (TextFilter<CompetitorDTO>) filter;
                textFilter.setOperator(new TextOperator(TextOperator.Operators.valueOf(operator)));
                textFilter.setValue(value);
            }
        }
        return filter;
    }
}
