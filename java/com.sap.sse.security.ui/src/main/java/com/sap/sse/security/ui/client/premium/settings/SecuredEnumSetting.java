package com.sap.sse.security.ui.client.premium.settings;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.common.settings.generic.StringToEnumConverter;
import com.sap.sse.common.settings.generic.converter.EnumConverter;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class SecuredEnumSetting<T extends Enum<T>> extends AbstractSecuredValueSetting<T> {

    public SecuredEnumSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings,
            StringToEnumConverter<T> stringToEnumConverter, Action action, SecurityChildSettingsContext securityContext) {
        this(name, settings, null, stringToEnumConverter, action, securityContext);
    }

    public SecuredEnumSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings, T defaultValue,
            StringToEnumConverter<T> stringToEnumConverter, Action action, SecurityChildSettingsContext securityContext) {
        super(name, settings, defaultValue, new EnumConverter<>(stringToEnumConverter), action, securityContext);
    }
}
