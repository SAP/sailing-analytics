package com.sap.sailing.domain.igtimiadapter.server.riot;

public enum RiotStandardCommand {
    CMD_POWER_OFF("POWER OFF"),
    CMD_GPS_OFF("GPS OFF"),
    CMD_GPS_ON("GPS ON"),
    CMD_RESTART("RESTART"),
    CMD_IMU_STOP("IMU STOP"),
    CMD_IMU_GYROCAL_PERFORM("IMU GYROCAL PERFORM"),
    CMD_IMU_CAL_FROM_FILE("IMU CAL FROM FILE"),
    CMD_IMU_SAVE("IMU SAVE"),
    CMD_IMU_ON("IMU ON");

    RiotStandardCommand(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }

    private final String command;
}
