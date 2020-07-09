package com.sap.sailing.domain.base.impl;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sse.common.impl.NamedImpl;

public class RemoteSailingServerReferenceImpl extends NamedImpl implements RemoteSailingServerReference {
    private static final long serialVersionUID = 3561284977118738878L;

    /** the URL of the server */
    private final URL url;
    private List<UUID> excludedEventIds;
    
    public RemoteSailingServerReferenceImpl(String name, URL url, List<UUID> excludedEventIds) {
        super(name);
        this.url = url;
        this.excludedEventIds = excludedEventIds;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public List<UUID> getExcludedEventIds() {
        return excludedEventIds;
    }
}