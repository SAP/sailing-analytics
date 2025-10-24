package com.sap.sailing.gwt.autoplay.client.app;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.IntegerSetting;

public class AutoplayPerspectiveOwnSettings extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -9013901094746556797L;

    private BooleanSetting fullscreen;
    private BooleanSetting switchToLive;
    private IntegerSetting timeToSwitchBeforeRaceStartInSeconds;
    private IntegerSetting waitTimeAfterRaceEndInSeconds;

    public AutoplayPerspectiveOwnSettings() {
        super();
    }

    public AutoplayPerspectiveOwnSettings(boolean fullscreen, boolean switchToLive,
            int timeToSwitchBeforeRaceStartInSeconds, int waitTimeAfterRaceEndInSeconds) {
        this();
        this.fullscreen.setValue(fullscreen);
        this.switchToLive.setValue(switchToLive);
        this.timeToSwitchBeforeRaceStartInSeconds.setValue(timeToSwitchBeforeRaceStartInSeconds);
        this.waitTimeAfterRaceEndInSeconds.setValue(waitTimeAfterRaceEndInSeconds);
    }

    @Override
    protected void addChildSettings() {
        fullscreen = new BooleanSetting("fullscreen", this, true);
        switchToLive = new BooleanSetting("switchToLive", this, true);
        timeToSwitchBeforeRaceStartInSeconds = new IntegerSetting("timeToSwitchBeforeRaceStart", this, 180);
        waitTimeAfterRaceEndInSeconds = new IntegerSetting("waitTimeAfterRaceEnd", this, 60);

    }

    public boolean isFullscreen() {
        return fullscreen.getValue();
    }

    public boolean isSwitchToLive() {
        return switchToLive.getValue();
    }

    public int getTimeToSwitchBeforeRaceStartInSeconds() {
        return timeToSwitchBeforeRaceStartInSeconds.getValue();
    }

    public int getWaitTimeAfterRaceEndInSeconds() {
        return waitTimeAfterRaceEndInSeconds.getValue();
    }
}
