package com.sap.sailing.server.replication;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.TimePoint;

/**
 * Describes a replica by remembering its IP address as well as the replication time and a UUID. Hash code and equality
 * are based solely on the UUID.
 * 
 * @author Frank Mittag, Axel Uhl (d043530)
 * 
 */
public class ReplicaDescriptor implements Serializable {
    private static final long serialVersionUID = -5451556877949921454L;

    private final UUID uuid;
    
    private final InetAddress ipAddress;
    
    private final TimePoint registrationTime;

    /**
     * Sets the registration time to now.
     */
    public ReplicaDescriptor(InetAddress ipAddress) {
        this.uuid = UUID.randomUUID();
        this.registrationTime = MillisecondsTimePoint.now();
        this.ipAddress = ipAddress;
    }

    public UUID getUuid() {
        return uuid;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public TimePoint getRegistrationTime() {
        return registrationTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReplicaDescriptor other = (ReplicaDescriptor) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
