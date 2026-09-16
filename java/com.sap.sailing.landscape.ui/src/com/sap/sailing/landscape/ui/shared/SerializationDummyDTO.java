package com.sap.sailing.landscape.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.landscape.common.EventLiveContent;
import com.sap.sailing.landscape.common.RaceLiveContent;
import com.sap.sailing.landscape.common.ReplicaSetLiveContent;

public class SerializationDummyDTO implements IsSerializable {
    public ProcessDTO mongoProcessDTO;
    public AwsInstanceDTO awsInstanceDTO;
    public SailingApplicationReplicaSetDTO<String> sailingApplicationReplicaSetDTO;
    public ReplicaSetLiveContent replicaSetLiveContent;
    public EventLiveContent eventLiveContent;
    public RaceLiveContent raceLiveContent;
    public Long lng;
}
