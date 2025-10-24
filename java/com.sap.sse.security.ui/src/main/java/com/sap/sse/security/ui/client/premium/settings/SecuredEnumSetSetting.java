package com.sap.sse.security.ui.client.premium.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.StreamSupport;

import com.google.gwt.core.client.GWT;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.common.settings.generic.StringToEnumConverter;
import com.sap.sse.common.settings.generic.base.AbstractValueSetSetting;
import com.sap.sse.common.settings.generic.converter.EnumConverter;
import com.sap.sse.common.settings.value.Value;
import com.sap.sse.common.settings.value.ValueCollectionValue;
import com.sap.sse.security.shared.HasPermissions.SecuredEnum;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

public class SecuredEnumSetSetting<T extends Enum<T> & SecuredEnum> extends AbstractValueSetSetting<T> {
    
    private final PaywallResolver paywallResolver;
    private final SecuredDTO securedDto;

    public SecuredEnumSetSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings,
            Iterable<T> defaultValues, StringToEnumConverter<T> stringToEnumConverter, SecurityChildSettingsContext securityContext) {
        super(name, settings, defaultValues, new EnumConverter<>(stringToEnumConverter));
        this.paywallResolver = securityContext.getPaywallResolver();
        this.securedDto = securityContext.getSecuredDTO();
    }

    /**
     * Return the values based on paywallResolver.hasPermission and Action from SecuredEnum. If no securedDTO is
     * available, all values are returned without hasPermission check (This is helpful e.g. for Autoplay, where at this
     * time of usage no specific DTO is available).
     */
    @Override
    public Iterable<T> getValues() {
        if (paywallResolver == null) {
            GWT.log("getValue paywall resolver not available.");
        }
        if (securedDto == null) {
            GWT.log("getValue securedDTO context not available!");
        }
        ValueCollectionValue<Set<Value>> value = getValue();
        Iterable<T> result = value.getValues(getValueConverter());
        Collection<T> permittedCollection = new ArrayList<T>();
        for (T singleValue: result) {
            if (securedDto == null 
                    || paywallResolver != null 
                    && paywallResolver.hasPermission(singleValue.getPremiumAction(), securedDto)) {
                permittedCollection.add(singleValue);
            } else {
                // handle default values
                if (StreamSupport.stream(getDefaultValues().spliterator(), false).anyMatch(t -> t == singleValue)) {
                    permittedCollection.add(singleValue);
                }
            }
        }
        return permittedCollection;
    }
}
