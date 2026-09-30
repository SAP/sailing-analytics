package com.sap.sailing.domain.coursetemplate;

import java.util.Map;
import java.util.UUID;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.coursetemplate.impl.FixedPositioningImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkPropertiesImpl;
import com.sap.sailing.domain.coursetemplate.impl.TrackingDeviceBasedPositioningImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class MarkPropertiesBuilder {
    private final UUID id;
    private final String name;
    private final String shortName;
    private final Color color;
    private final String shape;
    private final String pattern;
    private final MarkType type;
    private Iterable<String> tags;
    private Positioning positioningInformation;
    private Map<MarkTemplate, TimePoint> lastUsedTemplate;
    private Map<MarkRole, TimePoint> lastUsedRole;

    public MarkPropertiesBuilder(UUID id, String name, String shortName, Color color, String shape, String pattern,
            MarkType type) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.color = color;
        this.shape = shape;
        this.pattern = pattern;
        this.type = type;
    }

    public MarkPropertiesBuilder withTags(Iterable<String> tags) {
        this.tags = tags;
        return this;
    }

    public MarkPropertiesBuilder withDeviceId(DeviceIdentifier deviceId) {
        this.positioningInformation = new TrackingDeviceBasedPositioningImpl(deviceId);
        return this;
    }

    public MarkPropertiesBuilder withFixedPosition(Position position) {
        this.positioningInformation = new FixedPositioningImpl(position);
        return this;
    }

    public MarkProperties build() {
        MarkPropertiesImpl impl = new MarkPropertiesImpl(id, name, shortName, color, shape, pattern, type);
        if (tags != null) {
            impl.setTags(tags);
        }
        if (positioningInformation != null) {
            impl.setPositioningInformation(positioningInformation);
        }
        if (lastUsedTemplate != null) {
            impl.setLastUsedMarkTemplate(lastUsedTemplate);
        }
        if (lastUsedRole != null) {
            impl.setLastUsedMarkRole(lastUsedRole);
        }
        return impl;
    }

    public void withLastUsedTemplate(Map<MarkTemplate, TimePoint> lastUsedTemplate) {
        this.lastUsedTemplate = lastUsedTemplate;
    }

    public void withLastUsedRole(Map<MarkRole, TimePoint> lastUsedRole) {
        this.lastUsedRole = lastUsedRole;
    }

}
