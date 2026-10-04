package com.sap.sailing.gwt.home.communication.user.profile;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sse.gwt.dispatch.shared.commands.HasWriteAction;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;
import com.sap.sse.security.shared.UserManagementException;

public class SaveMiscEmailPreferences implements SailingAction<VoidResult>, HasWriteAction {
    private Boolean didOptOutOfFeatureAndCommunityEmails;

    protected SaveMiscEmailPreferences() {
    }

    public SaveMiscEmailPreferences(final Boolean didOptOutOfFeatureAndCommunityEmails) {
        this.didOptOutOfFeatureAndCommunityEmails = didOptOutOfFeatureAndCommunityEmails;
    }

    @Override
    @GwtIncompatible
    public VoidResult execute(SailingDispatchContext ctx) throws DispatchException {
        try {
            final String username = ctx.getSecurityService().getCurrentUser().getName();
            ctx.getSecurityService().updateUserProperties(username, null, null, null,
                    didOptOutOfFeatureAndCommunityEmails);
        } catch (UserManagementException e) {
            throw new DispatchException(e.getMessage());
        }
        return new VoidResult();
    }
}
