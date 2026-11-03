package com.sap.sailing.landscape;

import java.util.Optional;

import com.sap.sse.landscape.Log;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.CentralReverseProxy;
import com.sap.sse.landscape.aws.ReverseProxy;
import com.sap.sse.landscape.aws.common.shared.RedirectDTO;
import com.sap.sse.landscape.aws.common.shared.RedirectVisitor;

/**
 * From the respective {@link RedirectDTO} produces a corresponding {@link ReverseProxy} redirect that uses a process's
 * host's private IP address to route requests to it. This assumes that routing is enabled from the reverse proxy
 * to the process's host, either by both being within the same VPC or, e.g., by peering the VPCs they are in.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public abstract class AbstractALBToReverseProxyRedirectMapper<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, LogT extends Log>
implements RedirectVisitor {
    private final CentralReverseProxy<ShardingKey, MetricsT, ProcessT, LogT> centralReverseProxy;
    private final String hostname;
    private final Optional<String> optionalKeyName;
    private final byte[] privateKeyDecryptionPassphrase;
    
    public AbstractALBToReverseProxyRedirectMapper(CentralReverseProxy<ShardingKey, MetricsT, ProcessT, LogT> reverseProxy,
            String hostname, Optional<String> optionalKeyName, byte[] privateKeyDecryptionPassphrase) {
        super();
        this.centralReverseProxy = reverseProxy;
        this.hostname = hostname;
        this.optionalKeyName = optionalKeyName;
        this.privateKeyDecryptionPassphrase = privateKeyDecryptionPassphrase;
    }
    
    protected CentralReverseProxy<ShardingKey, MetricsT, ProcessT, LogT> getCentralReverseProxy() {
        return centralReverseProxy;
    }

    protected String getHostname() {
        return hostname;
    }

    protected Optional<String> getOptionalKeyName() {
        return optionalKeyName;
    }

    protected byte[] getPrivateKeyDecryptionPassphrase() {
        return privateKeyDecryptionPassphrase;
    }
}
