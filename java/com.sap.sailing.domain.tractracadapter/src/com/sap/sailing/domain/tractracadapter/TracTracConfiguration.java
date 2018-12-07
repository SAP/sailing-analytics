package com.sap.sailing.domain.tractracadapter;

import com.sap.sailing.domain.tractracadapter.security.TracTracSecuredDomainType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
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
    String getCourseDesignUpdateURI();

    /**
     * holds the Trac Trac username used to send course updates to TracTrac
     * @return the TracTrac username
     */
    String getTracTracUsername();

    /**
     * holds the Trac Trac password used to send course updates to TracTrac
     * @return the TracTrac password
     */
    String getTracTracPassword();

    @Override
    default QualifiedObjectIdentifier getIdentifier() {
        return getType().getQualifiedObjectIdentifier(getJSONURL());
    }

    @Override
    default HasPermissions getType() {
        return TracTracSecuredDomainType.TRACTRAC_ACCOUNT;
    }
}
