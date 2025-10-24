package com.sap.sse.security.ui.client;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettingsWithContext;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

/**
 * To be used, e.g., as context for {@link AbstractGenericSerializableSettingsWithContext}{@code .addChildSettings(...)}.
 */
public class SecurityChildSettingsContext {
    
    private final SecuredDTO securedDTO;
    
    private final PaywallResolver paywallResolver;
    
    public SecurityChildSettingsContext(SecuredDTO securedDTO, PaywallResolver paywallResolver) {
        this.securedDTO = securedDTO;
        this.paywallResolver = paywallResolver;
    }

    public SecuredDTO getSecuredDTO() {
        return securedDTO;
    }
    
    public PaywallResolver getPaywallResolver() {
        return paywallResolver;
    }
    
}
