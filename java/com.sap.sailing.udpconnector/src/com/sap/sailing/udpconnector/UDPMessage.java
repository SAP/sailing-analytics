package com.sap.sailing.udpconnector;

/**
 * A {@link UDPMessageParser} produces messages whose classes implement this interface. A parser is free to produce
 * messages it considers "invalid" for some reason. This, in turn, can be used when
 * {@link UDPReceiver#addListener(UDPMessageListener, boolean) registering listeners} where registrations may be
 * restricted to receiving {@link #isValid() valid} messages only.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface UDPMessage {
    boolean isValid();
}
