package com.sap.sailing.domain.common;

import com.sap.sailing.domain.common.i18n.DataImportProgressMessages;

public enum DataImportSubProgress {
    
    INIT {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.init();
        }
    },
    CONNECTION_SETUP {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.connectionSetup();
        }
    },
    CONNECTION_ESTABLISH {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.connectionEstablish();
        }
    },
    TRANSFER_STARTED {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.transferStarted();
        }
    },
    TRANSFER_COMPLETED {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.transferCompleted();
        }
    },
    IMPORT_INIT {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.importInit();
        }
    },
    IMPORT_WAIT {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.importWait();
        }
    },
    IMPORT_LEADERBOARD_GROUPS {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.importLeaderboardGroups();
        }
    },
    IMPORT_WIND_TRACKS {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.importWindTracks();
        }
    },
    IMPORT_SENSOR_FIXES {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.importSensorFixes();
        }
    },
    IMPORT_TRACKED_RACES {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.importTrackedRaces();
        }
    },
    WAITING_FOR_TRACKED_RACES_TO_FINISH_LOADING {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.waitingForTrackedRacesToFinishLoading();
        }
    },
    UPDATE_EVENT_LEADERBOARD_GROUP_LINKS {
        @Override
        public String getMessage(final DataImportProgressMessages messages) {
            return messages.updateEventLeaderboardGroupLinks();
        }
    };
    
    public abstract String getMessage(final DataImportProgressMessages messages);

}
