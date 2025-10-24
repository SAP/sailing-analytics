package com.sap.sailing.gwt.home.communication.event;

import com.sap.sailing.gwt.ui.client.StringMessages;

public enum LabelType {
    NONE(null) {
        @Override
        public String getLabel() {
            return null;
        }
    },
    LIVE("live") {
        @Override
        public String getLabel() {
            return StringMessages.INSTANCE.live();
        }
    },
    FINISHED("finished") {
        @Override
        public String getLabel() {
            return StringMessages.INSTANCE.finished();
        }
    },
    PROGRESS("progress") {
        @Override
        public String getLabel() {
            return StringMessages.INSTANCE.inProgress();
        }
    },
    UPCOMING("upcoming") {
        @Override
        public String getLabel() {
            return StringMessages.INSTANCE.upcoming();
        }
    },
    ACTIVE("active") {
        @Override
        public String getLabel() {
            return StringMessages.INSTANCE.active();
        }
    },
    IN_TRIAL("intrial") {
        @Override
        public String getLabel() {
            return StringMessages.INSTANCE.inTrial();
        }
    },
    CANCELLED("cancelled") {
        @Override
        public String getLabel() {
            return StringMessages.INSTANCE.cancelled();
        }
    },
    UNKNOWN("unknown") {
        @Override
        public String getLabel() {
            return StringMessages.INSTANCE.unknown();
        }
    };

    private final String labelType;

    private LabelType() {
        // For GWT serialization only
        labelType = null;
    }

    private LabelType(final String labelType) {
        this.labelType = labelType;
    }

    public abstract String getLabel();

    public String getLabelType() {
        return labelType;
    }

    public boolean isRendered() {
        return labelType != null && getLabel() != null;
    }
}
