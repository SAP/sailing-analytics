package com.sap.sse.security.ui.client.premium;

public class PaywallResolverProxy {

    public PaywallResolverProxy(PaywallResolver paywallResolver) {
        this.paywallResolver = paywallResolver;
    }

    PaywallResolver paywallResolver;

    public PaywallResolver getPaywallResolver() {
        return paywallResolver;
    }

    public void setPaywallResolver(PaywallResolver paywallResolver) {
        this.paywallResolver = paywallResolver;
    }
}
