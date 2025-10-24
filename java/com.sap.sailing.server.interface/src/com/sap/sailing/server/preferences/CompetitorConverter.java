package com.sap.sailing.server.preferences;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sse.common.settings.generic.ValueConverter;
import com.sap.sse.common.settings.value.StringValue;
import com.sap.sse.common.settings.value.Value;

public class CompetitorConverter implements ValueConverter<Competitor> {

    private final CompetitorAndBoatStore competitorStore;

    public CompetitorConverter(CompetitorAndBoatStore competitorStore) {
        this.competitorStore = competitorStore;
    }

    @Override
    public Object toJSONValue(Competitor value) {
        return toStringValue(value);
    }

    @Override
    public Competitor fromJSONValue(Object jsonValue) {
        return fromStringValue((String) jsonValue);
    }

    @Override
    public String toStringValue(Competitor value) {
        return value == null ? null : value.getId().toString();
    }

    @Override
    public Competitor fromStringValue(String stringValue) {
        return stringValue == null ? null : competitorStore.getExistingCompetitorByIdAsString(stringValue);
    }

    @Override
    public Competitor fromValue(Value value) {
        return fromStringValue(((StringValue) value).getValue());
    }

    @Override
    public Value toValue(Competitor value) {
        return new StringValue(toStringValue(value));
    }
}