package com.sap.sse.security.operations;

import java.util.Locale;

import com.sap.sse.security.impl.ReplicableSecurityService;

public class UpdateUserPropertiesOperation implements SecurityOperation<Void> {
    private static final long serialVersionUID = -6267523788529623080L;
    protected final String username;
    /** When null, an update to this property will not be processed */
    protected final String fullName;
    /** When null, an update to this property will not be processed */
    protected final String company;
    /** When null, an update to this property will not be processed */
    protected final Locale locale;
    /** When null, an update to this property will not be processed */
    protected final Boolean didOptOutOfFeatureAndCommunityEmails;

    public UpdateUserPropertiesOperation(String username, String fullName, String company, Locale locale,
            Boolean didOptOutOfFeatureAndCommunityEmails) {
        this.username = username;
        this.fullName = fullName;
        this.company = company;
        this.locale = locale;
        this.didOptOutOfFeatureAndCommunityEmails = didOptOutOfFeatureAndCommunityEmails;
    }

    @Override
    public Void internalApplyTo(ReplicableSecurityService toState) throws Exception {
        toState.internalUpdateUserProperties(username, fullName, company, locale, didOptOutOfFeatureAndCommunityEmails);
        return null;
    }
}
