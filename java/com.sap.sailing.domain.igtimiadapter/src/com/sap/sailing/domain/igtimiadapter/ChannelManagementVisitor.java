package com.sap.sailing.domain.igtimiadapter;

import java.util.logging.Logger;

import com.igtimi.IgtimiStream.Authentication;
import com.igtimi.IgtimiStream.ChannelManagement;
import com.igtimi.IgtimiStream.DataSubscription;
import com.igtimi.IgtimiStream.ServerDisconnecting;

public interface ChannelManagementVisitor {
    static final Logger logger = Logger.getLogger(ChannelManagementVisitor.class.getName());

    static void accept(ChannelManagement msg, ChannelManagementVisitor visitor) {
        switch (msg.getMgmtCase()) {
        case AUTH:
            visitor.handleAuth(msg.getAuth());
            break;
        case DISCONNECT:
            visitor.handleDisconnect(msg.getDisconnect());
            break;
        case HEARTBEAT:
            visitor.handleHeartbeat(msg.getHeartbeat());
            break;
        case SUBSCRIPTION:
            visitor.handleSubscription(msg.getSubscription());
            break;
        case MGMT_NOT_SET:
            logger.warning("Unknown message type " + msg.getMgmtCase());
            break;
        default:
        }
    }

    default void handleSubscription(DataSubscription subscription) {
    }

    default void handleHeartbeat(long heartbeat) {
    }

    default void handleDisconnect(ServerDisconnecting disconnect) {
    }

    default void handleAuth(Authentication auth) {
    }

}
