package com.sap.sailing.server.preferences;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.base.AbstractValueSetSetting;

/**
 * Specific setting to hold a set of {@link Competitor} instance. The competitors are serialized as ID and deserialized
 * doing a lookup in the given {@link CompetitorAndBoatStore}.
 */
public class CompetitorSetSetting extends AbstractValueSetSetting<Competitor> {

    protected CompetitorSetSetting(String name, AbstractGenericSerializableSettings settings,
            CompetitorAndBoatStore competitorStore) {
        super(name, settings, new CompetitorConverter(competitorStore));
    }
}
