package com.sap.sailing.gwt.ui.adminconsole;

import com.sap.sse.common.TimedLock;

public class IpToTimedLockDTO {
    public final String ip;
    public final TimedLock timedLock;

    public IpToTimedLockDTO(final String ip, final TimedLock timedLock) {
        this.ip = ip;
        this.timedLock = timedLock;
    }

}
