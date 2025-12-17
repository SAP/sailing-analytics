package com.sap.sse.security.ui.shared;

import com.sap.sse.common.TimedLock;
import com.sap.sse.common.Named;

public class IpToTimedLockDTO implements Named {
    private static final long serialVersionUID = 7877190394556881643L;
    private final String ip;
    private final TimedLock timedLock;

    public IpToTimedLockDTO(final String ip, final TimedLock timedLock) {
        this.ip = ip;
        this.timedLock = timedLock;
    }

    @Override
    public String getName() {
        return "IpToTimedLockDTO";
    }

    public String getIp() {
        return ip;
    }

    public TimedLock getTimedLock() {
        return timedLock;
    }
}
