package com.sap.sailing.datamining.impl.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sap.sailing.datamining.data.SailorProfile;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.NamedWithUUIDImpl;

public class SailorProfileImpl extends NamedWithUUIDImpl implements SailorProfile {
    private static final long serialVersionUID = 5851705759679719738L;

    private final List<Competitor> competitors;
    
    public SailorProfileImpl(UUID id, String name, Iterable<Competitor> competitors) {
        super(name, id);
        final List<Competitor> myCompetitors = new ArrayList<>();
        Util.addAll(competitors, myCompetitors);
        this.competitors = myCompetitors;
    }

    @Override
    public Iterable<Competitor> getCompetitors() {
        return Collections.unmodifiableList(competitors);
    }
}
