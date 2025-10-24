package com.sap.sse.security.ui.client.premium.settings;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.common.settings.generic.converter.StringConverter;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class SecuredStringSetting extends AbstractSecuredValueSetting<String> {

    public SecuredStringSetting(final String name, final AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings,
            Action action, SecurityChildSettingsContext securityContext) {
        this(name, settings, null, action, securityContext);
    }

    public SecuredStringSetting(final String name, final AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings,
            final String defaultValue, Action action, SecurityChildSettingsContext securityContext) {
        super(name, settings, defaultValue, StringConverter.INSTANCE, action, securityContext);
    }

    public boolean isNotBlank() {
        final String value = getValue();
        return value != null && !value.isEmpty();
    }
}
