package com.sap.sse.security.ui.client.premium.settings;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.common.settings.generic.converter.LongConverter;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class SecuredLongSetting extends AbstractSecuredValueSetting<Long> {

    public SecuredLongSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings,
            Action action, SecurityChildSettingsContext securityContext) {
        this(name, settings, null, action, securityContext);
    }

    public SecuredLongSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings, Long defaultValue,
            Action action, SecurityChildSettingsContext securityContext) {
        super(name, settings, defaultValue, LongConverter.INSTANCE, action, securityContext);
    }
}
