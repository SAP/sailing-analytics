package com.sap.sailing.domain.base.configuration.impl;

import java.io.Serializable;
import java.util.Arrays;

import com.sap.sailing.domain.base.IsManagedBySharedDomainFactory;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationIdentifier;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationMatcher;

/**
 * Matches a single {@link DeviceConfigurationIdentifier}.
 */
public class DeviceConfigurationMatcherSingle implements DeviceConfigurationMatcher {
    
    private static final long serialVersionUID = -3662453614818373801L;
    private final String clientIdentifier;

    public DeviceConfigurationMatcherSingle(String matchingClientIdentifier) {
        this.clientIdentifier = matchingClientIdentifier;
    }

    @Override
    public boolean matches(DeviceConfigurationIdentifier identifier) {
        return this.clientIdentifier.equals(identifier.getClientIdentifier());
    }

    @Override
    public Type getMatcherType() {
        return Type.SINGLE;
    }

    @Override
    public int getMatchingRank() {
        return getMatcherType().getRank();
    }
    
    public String getClientIdentifier() {
        return clientIdentifier;
    }

    @Override
    public Serializable getMatcherIdentifier() {
        return String.format("%s%s", DeviceConfigurationMatcherSingle.class.getSimpleName(), clientIdentifier);
    }

    @Override
    public String toString() {
        return "DeviceConfigurationMatcherSingle [clientIdentifier=" + clientIdentifier + "]";
    }

    @Override
    public IsManagedBySharedDomainFactory resolve(SharedDomainFactory domainFactory) {
        return domainFactory.getOrCreateDeviceConfigurationMatcher(getMatcherType(), Arrays.asList(clientIdentifier));
    }

}
