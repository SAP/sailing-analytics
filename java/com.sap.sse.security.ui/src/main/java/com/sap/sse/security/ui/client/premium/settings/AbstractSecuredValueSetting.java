package com.sap.sse.security.ui.client.premium.settings;

import com.google.gwt.core.client.GWT;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.common.settings.generic.ValueConverter;
import com.sap.sse.common.settings.generic.base.AbstractValueSetting;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

public abstract class AbstractSecuredValueSetting<T> extends AbstractValueSetting<T> {

    private final PaywallResolver paywallResolver;
    private final Action action;
    private final SecuredDTO securedDTO;

    protected AbstractSecuredValueSetting(String name, AbstractGenericSerializableSettingsWithContext<SecurityChildSettingsContext> settings, T defaultValue,
            ValueConverter<T> valueConverter, Action action, SecurityChildSettingsContext securityContext) {
        super(name, settings, defaultValue, valueConverter);
        this.paywallResolver = securityContext.getPaywallResolver();
        this.action = action;
        this.securedDTO = securityContext.getSecuredDTO();
    }
    
    public boolean hasPermission() {
        return paywallResolver.hasPermission(action, securedDTO);
    }

    @Override
    public T getValue() {
        if (paywallResolver == null) {
            GWT.log("getValue paywall resolver not available.");
        }
        if (securedDTO == null) {
            GWT.log("getValue securedDTO context not available!");
        }
        if (securedDTO == null 
                || paywallResolver != null 
                && paywallResolver.hasPermission(action, securedDTO)) {
            return super.getValue();
        } else {
            return super.getDefaultValue();
        }
    }

    @Override
    public void setValue(T value) {
        if (paywallResolver == null) {
            GWT.log("setValue Paywall resolver not available.");
        }
        if (securedDTO == null) {
            GWT.log("setValue DTOContext not set!");
        }
        if (securedDTO != null
                ||  paywallResolver != null && paywallResolver.hasPermission(action, securedDTO)) {
            super.setValue(value);
        }
    }

    public PaywallResolver getPaywallResolver() {
        return paywallResolver;
    }

    public Action getAction() {
        return action;
    }

    public SecuredDTO getSecuredDTO() {
        return securedDTO;
    }
}
