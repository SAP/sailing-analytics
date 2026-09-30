package com.sap.sailing.landscape.ui.shared;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.common.Named;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.landscape.aws.common.shared.RedirectDTO;
import com.sap.sse.security.ui.client.UserService;

public class SailingApplicationReplicaSetDTO<ShardingKey> implements Named, IsSerializable {
    private static final long serialVersionUID = 8449684019896974806L;
    private String replicaSetName;
    private SailingAnalyticsProcessDTO master;
    private ArrayList<SailingAnalyticsProcessDTO> replicas;
    private String version;
    private String releaseNotesLink;
    private String hostname;
    /**
     * Originates from load balancer rule and therefore it can be null of not managed by load balancer rule
     */
    private String defaultRedirectPath;
    private String autoScalingGroupAmiId;
    
    @Deprecated
    SailingApplicationReplicaSetDTO() {} // for GWT RPC serialization only

    public SailingApplicationReplicaSetDTO(String replicaSetName, SailingAnalyticsProcessDTO master,
            Iterable<SailingAnalyticsProcessDTO> replicas, String version, String releaseNotesLink, String hostname,
            String defaultRedirectPath, String autoScalingGroupAmiId) {
        super();
        this.master = master;
        this.replicaSetName = replicaSetName;
        this.version = version;
        this.replicas = new ArrayList<>();
        this.releaseNotesLink = releaseNotesLink;
        this.hostname = hostname;
        this.defaultRedirectPath = defaultRedirectPath;
        this.autoScalingGroupAmiId = autoScalingGroupAmiId;
        Util.addAll(replicas, this.replicas);
    }
    
    /**
     * Same as {@link #getReplicaSetName()}, adapting to the {@link Named} interface
     */
    public String getName() {
        return getReplicaSetName();
    }

    public String getReplicaSetName() {
        return replicaSetName;
    }

    public SailingAnalyticsProcessDTO getMaster() {
        return master;
    }

    public ArrayList<SailingAnalyticsProcessDTO> getReplicas() {
        return replicas;
    }

    public String getVersion() {
        return version;
    }

    public String getReleaseNotesLink() {
        return releaseNotesLink;
    }

    /**
     * @return a fully-qualified hostname which can, e.g., be used to look up the load balancer taking the requests for
     *         this application replica set.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @return may be null if not managed by load balancer rule 
     */  
    public String getDefaultRedirectPath() {
        return defaultRedirectPath;
    }

    public String getAutoScalingGroupAmiId() {
        return autoScalingGroupAmiId;
    }

    /**
     * From the {@link #getDefaultRedirectPath() defaultRedirectPath} infers a {@link RedirectDTO} describing
     * the default redirection used by this replica set.
     */
    public RedirectDTO getDefaultRedirect() {
        return RedirectDTO.from(getDefaultRedirectPath());
    }
    
    /**
     * @return {@code true} if this replica set is the one that this method is being run on. See {@link ServerInfoDTO}
     *         and {@link UserService#getServerInfo()}.
     */
    public boolean isLocalReplicaSet(UserService userService) {
        return getName().equals(userService.getServerInfo().getName());
    }

    /**
     * Based on this replica set's {@link #getName() name} decides whether this is the ARCHIVE.
     *
     * @see SharedLandscapeConstants#ARCHIVE_SERVER_APPLICATION_REPLICA_SET_NAME
     */
    public boolean isArchive() {
        return getName().equals(SharedLandscapeConstants.ARCHIVE_SERVER_APPLICATION_REPLICA_SET_NAME);
    }

    @Override
    public String toString() {
        return "SailingApplicationReplicaSetDTO [replicaSetName=" + replicaSetName + ", master=" + master
                + ", replicas=" + replicas + ", version=" + version + ", hostname=" + hostname
                + ", defaultRedirectPath=" + defaultRedirectPath + ", autoScalingGroupAmiId=" + autoScalingGroupAmiId
                + "]";
    }
}
