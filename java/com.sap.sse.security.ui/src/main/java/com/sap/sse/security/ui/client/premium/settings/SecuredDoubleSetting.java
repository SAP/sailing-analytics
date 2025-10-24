package com.sap.sse.security.ui.client.premium.settings;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.common.settings.generic.converter.DoubleConverter;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class SecuredDoubleSetting extends AbstractSecuredValueSetting<Double> {

    public SecuredDoubleSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings,
            Action action, SecurityChildSettingsContext securityContext) {
        this(name, settings, null, action, securityContext);
    }

    public SecuredDoubleSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings, Double defaultValue,
            Action action, SecurityChildSettingsContext securityContext) {
        super(name, settings, defaultValue, DoubleConverter.INSTANCE, action, securityContext);
    }
}
