package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.InputStream;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sse.common.ObjectInputStreamResolvingAgainstCache;

public class ObjectInputStreamResolvingAgainstDomainFactoryImpl extends ObjectInputStreamResolvingAgainstCache<DomainFactory> {
    ObjectInputStreamResolvingAgainstDomainFactoryImpl(InputStream in, DomainFactory domainFactory) throws IOException {
        super(in, domainFactory);
    }
}
