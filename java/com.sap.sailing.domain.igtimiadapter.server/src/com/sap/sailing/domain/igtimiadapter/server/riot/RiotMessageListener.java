package com.sap.sailing.domain.igtimiadapter.server.riot;

import com.igtimi.IgtimiStream.Msg;

/**
 * A listener that can be {@link RiotServer#addListener added} to a {@link RiotServer} to
 * get notified when the server has received a message from a device.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface RiotMessageListener {
    void onMessage(Msg message);
}
