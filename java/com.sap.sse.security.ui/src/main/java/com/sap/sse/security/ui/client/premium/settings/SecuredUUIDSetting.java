package com.sap.sse.security.ui.client.premium.settings;

import java.util.UUID;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.common.settings.generic.converter.UUIDConverter;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class SecuredUUIDSetting extends AbstractSecuredValueSetting<UUID> {

    public SecuredUUIDSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings,
            Action action, SecurityChildSettingsContext securityContext) {
        this(name, settings, null, action, securityContext);
    }

    public SecuredUUIDSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings, UUID defaultValue,
            Action action, SecurityChildSettingsContext securityContext) {
        super(name, settings, defaultValue, UUIDConverter.INSTANCE, action, securityContext);
    }
}
