package com.sap.sse.security.ui.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.TimedLock;
import com.sap.sse.landscape.aws.common.shared.SecuredAwsLandscapeType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

public class SerializationDummy implements IsSerializable {
    TypeRelativeObjectIdentifier typeRelativeObjectIdentifier;
    HasPermissions hasPermissions;
    SecuredAwsLandscapeType securedAwsLandscapeType;
    TimedLock timedLock;
}
