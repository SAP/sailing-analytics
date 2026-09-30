package com.sap.sailing.gwt.ui.shared.courseCreation;

import java.util.ArrayList;
import java.util.Arrays;

import com.sap.sailing.gwt.ui.shared.DeviceIdentifierDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Triple;

public abstract class MarkConfigurationDTO extends ControlPointWithMarkConfigurationDTO {
    private static final long serialVersionUID = -6944495722826431655L;
    private MarkTemplateDTO optionalMarkTemplate;
    private boolean addToMarkPropertiesInventoryRequest;
    private boolean createMarkRoleRequest;
    private Position fixedPositionRequest;
    private DeviceIdentifierDTO deviceMappingRequest;
    private GPSFixDTO lastKnownPosition;
    private ArrayList<Triple<DeviceIdentifierDTO, TimeRange, GPSFixDTO>> existingDeviceMappings;

    public MarkTemplateDTO getOptionalMarkTemplate() {
        return optionalMarkTemplate;
    }

    public void setOptionalMarkTemplate(MarkTemplateDTO optionalMarkTemplate) {
        this.optionalMarkTemplate = optionalMarkTemplate;
    }

    public abstract MarkPropertiesDTO getOptionalMarkProperties();

    public abstract CommonMarkPropertiesDTO getEffectiveProperties();

    @Override
    public Iterable<MarkConfigurationDTO> getMarkConfigurations() {
        return Arrays.asList(this);
    }

    public boolean isAddToMarkPropertiesInventoryRequest() {
        return addToMarkPropertiesInventoryRequest;
    }

    public void setAddToMarkPropertiesInventoryRequest(boolean addToMarkPropertiesInventoryRequest) {
        this.addToMarkPropertiesInventoryRequest = addToMarkPropertiesInventoryRequest;
    }

    public boolean isCreateMarkRoleRequest() {
        return createMarkRoleRequest;
    }

    public void setCreateMarkRoleRequest(boolean createMarkRoleRequest) {
        this.createMarkRoleRequest = createMarkRoleRequest;
    }

    public Position getFixedPositionRequest() {
        return fixedPositionRequest;
    }

    public void setFixedPositionRequest(Position fixedPositionRequest) {
        this.fixedPositionRequest = fixedPositionRequest;
    }

    public DeviceIdentifierDTO getDeviceMappingRequest() {
        return deviceMappingRequest;
    }

    public void setDeviceMappingRequest(DeviceIdentifierDTO deviceMappingRequest) {
        this.deviceMappingRequest = deviceMappingRequest;
    }

    public GPSFixDTO getLastKnownPosition() {
        return lastKnownPosition;
    }

    public void setLastKnownPosition(GPSFixDTO lastKnownPosition) {
        this.lastKnownPosition = lastKnownPosition;
    }

    public ArrayList<Triple<DeviceIdentifierDTO, TimeRange, GPSFixDTO>> getExistingDeviceMappings() {
        return existingDeviceMappings;
    }

    public void setExistingDeviceMappings(ArrayList<Triple<DeviceIdentifierDTO, TimeRange, GPSFixDTO>> existingDeviceMappings) {
        this.existingDeviceMappings = existingDeviceMappings;
    }
}
