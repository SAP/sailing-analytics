package com.sap.sse.security.ui.client.subscription;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.security.ui.shared.subscription.SubscriptionListDTO;

public interface SubscriptionWriteServiceAsync<C, P, F> extends SubscriptionServiceAsync<C, P> {
    public void finishCheckout(F checkoutData, AsyncCallback<SubscriptionListDTO> result);

    public void cancelSubscription(String planId, AsyncCallback<Boolean> result);
}
