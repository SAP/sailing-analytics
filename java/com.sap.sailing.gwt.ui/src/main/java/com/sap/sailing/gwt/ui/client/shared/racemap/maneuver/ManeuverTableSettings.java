package com.sap.sailing.gwt.ui.client.shared.racemap.maneuver;

import java.util.Arrays;
import java.util.Set;

import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sse.common.Util;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.EnumSetSetting;

public class ManeuverTableSettings extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -3250243915670349222L;

    private EnumSetSetting<ManeuverType> selectedManeuverTypes;

    @Override
    protected void addChildSettings() {
        selectedManeuverTypes = new EnumSetSetting<>("selectedManeuverTypes", this,
                Arrays.asList(ManeuverType.TACK, ManeuverType.JIBE, ManeuverType.PENALTY_CIRCLE),
                ManeuverType::valueOf);
    }

    /**
     * The default settings
     */
    public ManeuverTableSettings() {
        super();
    }

    public ManeuverTableSettings(Set<ManeuverType> selectedManeuverTypes) {
        this();
        this.selectedManeuverTypes.setValues(selectedManeuverTypes);
    }

    public Set<ManeuverType> getSelectedManeuverTypes() {
        return Util.asSet(selectedManeuverTypes.getValues());
    }
}
