package com.sap.sse.security.impl;

import java.io.IOException;
import java.io.InputStream;

import com.sap.sse.common.ObjectInputStreamResolvingAgainstCache;
import com.sap.sse.security.UserStore;

public class ObjectInputStreamResolvingAgainstSecurityCache extends ObjectInputStreamResolvingAgainstCache<UserStore> {
    ObjectInputStreamResolvingAgainstSecurityCache(InputStream in, UserStore cache) throws IOException {
        super(in, cache);
    }
}
