package com.sap.sailing.landscape;

import java.util.Optional;

import com.sap.sse.landscape.Log;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.CentralReverseProxy;
import com.sap.sse.landscape.aws.ReverseProxy;
import com.sap.sse.landscape.aws.common.shared.EventRedirectDTO;
import com.sap.sse.landscape.aws.common.shared.EventSeriesRedirectDTO;
import com.sap.sse.landscape.aws.common.shared.HomeRedirectDTO;
import com.sap.sse.landscape.aws.common.shared.PlainRedirectDTO;
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
public class ALBToReverseProxyRedirectMapper<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, LogT extends Log>
extends AbstractALBToReverseProxyRedirectMapper<ShardingKey, MetricsT, ProcessT, LogT>
implements RedirectVisitor {
    private final ProcessT master;
    
    public ALBToReverseProxyRedirectMapper(CentralReverseProxy<ShardingKey, MetricsT, ProcessT, LogT> reverseProxy,
            String hostname, ProcessT master, Optional<String> optionalKeyName, byte[] privateKeyDecryptionPassphrase) {
        super(reverseProxy, hostname, optionalKeyName, privateKeyDecryptionPassphrase);
        this.master = master;
    }

    @Override
    public void visit(PlainRedirectDTO plainRedirectDTO) throws Exception {
        getCentralReverseProxy().setPlainRedirect(getHostname(), master, getOptionalKeyName(), getPrivateKeyDecryptionPassphrase());
    }

    @Override
    public void visit(HomeRedirectDTO homeRedirectDTO) throws Exception {
        getCentralReverseProxy().setHomeRedirect(getHostname(), master, getOptionalKeyName(), getPrivateKeyDecryptionPassphrase());
    }

    @Override
    public void visit(EventRedirectDTO eventRedirectDTO) throws Exception {
        getCentralReverseProxy().setEventRedirect(getHostname(), master, eventRedirectDTO.getId(), getOptionalKeyName(), getPrivateKeyDecryptionPassphrase());
    }

    @Override
    public void visit(EventSeriesRedirectDTO eventSeriesRedirectDTO) throws Exception {
        getCentralReverseProxy().setEventSeriesRedirect(getHostname(), master, eventSeriesRedirectDTO.getId(), getOptionalKeyName(), getPrivateKeyDecryptionPassphrase());
    }
}
