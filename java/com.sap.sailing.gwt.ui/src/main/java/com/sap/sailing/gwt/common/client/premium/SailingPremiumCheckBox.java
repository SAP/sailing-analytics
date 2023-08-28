package com.sap.sailing.gwt.common.client.premium;

import static com.sap.sailing.gwt.common.client.premium.SailingPremiumIconRessource.INSTANCE;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.sap.sailing.gwt.ui.client.EntryPointLinkFactory;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.client.premium.PremiumCheckBox;

public class SailingPremiumCheckBox extends PremiumCheckBox {

    public SailingPremiumCheckBox(final String label, final Action action, final PaywallResolverImpl paywallResolver, SecuredDTO contextDTO) {
        super(label, action, paywallResolver, contextDTO);
    }

    @Override
    protected Image createPremiumIcon() {
        return new Image();
    }

    @Override
    protected void onUserPermissionUpdate(final boolean isPermitted) {
        super.onUserPermissionUpdate(isPermitted);
        image.setUrl((isPermitted ? INSTANCE.premiumIconPermitted() : INSTANCE.premiumIcon()).getSafeUri());
    }

    @Override
    protected void onSubscribeDialogConfirmation(final Iterable<String> unlockingPlans) {
        Window.open(EntryPointLinkFactory.createSubscriptionPageLink(unlockingPlans), "_blank", "");
    }

}
