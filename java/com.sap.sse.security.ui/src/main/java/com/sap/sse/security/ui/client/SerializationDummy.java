package com.sap.sse.security.ui.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.landscape.aws.common.shared.SecuredAwsLandscapeType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

public class SerializationDummy implements IsSerializable {
    public TypeRelativeObjectIdentifier typeRelativeObjectIdentifier;
    public HasPermissions hasPermissions;
    public SecuredAwsLandscapeType securedAwsLandscapeType;
}
