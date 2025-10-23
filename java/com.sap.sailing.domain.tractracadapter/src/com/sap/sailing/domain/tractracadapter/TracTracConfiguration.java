package com.sap.sailing.domain.tractracadapter;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;

/**
 * Configuration parameters that can be used to connect to a TracTrac event / race.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface TracTracConfiguration extends WithQualifiedObjectIdentifier {
    String getName();
    
    String getJSONURL();
    
    String getLiveDataURI();
    
    String getStoredDataURI();
    
    /**
     * holds the path of Trac Trac to receive course updates triggered by the race committee
     * @return the TracTrac server path for course updates
     */
    String getUpdateURI();

    /**
     * holds the Trac Trac API token authenticating a used, used to send course updates to TracTrac
     */
    String getTracTracApiToken();

    String getCreatorName();

    @Override
    default QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    @Override
    default HasPermissions getPermissionType() {
        return SecuredDomainType.TRACTRAC_ACCOUNT;
    }

    // TODO it would be nice to factor the redundancy with TracTracConfigurationWithSecurityDTO.getTypeRelativeObjectIdentifier but it would require introducing a new .common bundle
    default TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(getJSONURL(), getCreatorName());
    }

    static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(String jsonUrl, String username) {
        return username == null ? new TypeRelativeObjectIdentifier(jsonUrl)
                : new TypeRelativeObjectIdentifier(jsonUrl, username);
    }
}
