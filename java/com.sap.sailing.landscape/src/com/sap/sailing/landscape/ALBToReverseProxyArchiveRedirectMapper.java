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
 * From the respective {@link RedirectDTO} produces a corresponding {@link ReverseProxy} redirect that maps to the ARCHIVE server.
 * For the "Plain" / index.html routing we don't currently have a corresponding ARCHIVE rewrite macro, hence a plain
 * routing is mapped to a Home.html rewrite for the ARCHIVE.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ALBToReverseProxyArchiveRedirectMapper<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, LogT extends Log>
extends AbstractALBToReverseProxyRedirectMapper<ShardingKey, MetricsT, ProcessT, LogT>
implements RedirectVisitor {
    
    public ALBToReverseProxyArchiveRedirectMapper(CentralReverseProxy<ShardingKey, MetricsT, ProcessT, LogT> reverseProxy,
            String hostname, Optional<String> optionalKeyName, byte[] privateKeyDecryptionPassphrase) {
        super(reverseProxy, hostname, optionalKeyName, privateKeyDecryptionPassphrase);
    }

    @Override
    public void visit(PlainRedirectDTO plainRedirectDTO) throws Exception {
        getCentralReverseProxy().setHomeArchiveRedirect(getHostname(), getOptionalKeyName(), getPrivateKeyDecryptionPassphrase());
    }

    @Override
    public void visit(HomeRedirectDTO homeRedirectDTO) throws Exception {
        getCentralReverseProxy().setHomeArchiveRedirect(getHostname(), getOptionalKeyName(), getPrivateKeyDecryptionPassphrase());
    }

    @Override
    public void visit(EventRedirectDTO eventRedirectDTO) throws Exception {
        getCentralReverseProxy().setEventArchiveRedirect(getHostname(), eventRedirectDTO.getId(), getOptionalKeyName(), getPrivateKeyDecryptionPassphrase());
    }

    @Override
    public void visit(EventSeriesRedirectDTO eventSeriesRedirectDTO) throws Exception {
        getCentralReverseProxy().setEventSeriesArchiveRedirect(getHostname(), eventSeriesRedirectDTO.getId(), getOptionalKeyName(), getPrivateKeyDecryptionPassphrase());
    }
}
