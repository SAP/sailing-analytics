package com.sap.sailing.domain.swisstimingadapter.persistence.impl;

public enum FieldNames {
    // SwissTiming configuration parameters:
    ST_CONFIG_NAME, ST_CONFIG_HOSTNAME, ST_CONFIG_PORT, ST_CONFIG_CAN_SEND_REQUESTS,
    
    // last message count field:
    LAST_MESSAGE_COUNT,
    
    // raw messages:
    MESSAGE_SEQUENCE_NUMBER, MESSAGE_CONTENT, MESSAGE_COMMAND,
    
    // race specific message and masterdata
    RACE_ID, RACE_DESCRIPTION, RACE_STARTTIME,
}
