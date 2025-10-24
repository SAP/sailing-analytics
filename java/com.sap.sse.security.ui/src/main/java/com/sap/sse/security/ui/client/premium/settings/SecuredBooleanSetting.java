package com.sap.sse.security.ui.client.premium.settings;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.common.settings.generic.converter.BooleanConverter;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class SecuredBooleanSetting extends AbstractSecuredValueSetting<Boolean> {

    public SecuredBooleanSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings,
            Action action, SecurityChildSettingsContext securityContext) {
        this(name, settings, null, action, securityContext);
    }

    public SecuredBooleanSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings, Boolean defaultValue,
            Action action, SecurityChildSettingsContext securityContext) {
        super(name, settings, defaultValue, BooleanConverter.INSTANCE, action, securityContext);
    }
}
