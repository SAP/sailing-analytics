package com.sap.sailing.gwt.common.communication.event;

import java.util.Date;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.shared.EventReferenceDTO;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.SecurityInformationDTO;

public class EventMetadataDTO extends EventReferenceDTO implements SecuredDTO {

    private static final long serialVersionUID = 8054542216903623964L;

    private EventState state;
    private String venue;
    private String location;
    private Date startDate;
    private Date endDate;
    private String thumbnailImageURL;
    private String name;
    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();

    private Date currentServerTime;

    public EventMetadataDTO() {
        initCurrentServerTime();
    }

    public boolean isRunning() {
        return state == EventState.RUNNING;
    }
    
    public boolean isStarted() {
        return state.compareTo(EventState.RUNNING) >= 0;
    }
    
    public boolean isFinished() {
        return state == EventState.FINISHED;
    }

    private void initCurrentServerTime() {
        currentServerTime = new Date();
    }

    public Date getCurrentServerTime() {
        return currentServerTime;
    }

    public EventState getState() {
        return state;
    }
    
    public void setState(EventState state) {
        this.state = state;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getLocation() {
        return location;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getThumbnailImageURL() {
        return thumbnailImageURL;
    }

    public void setThumbnailImageURL(String thumbnailImageURL) {
        this.thumbnailImageURL = thumbnailImageURL;
    }

    public String getLocationOrVenue() {
        if(location != null && !location.isEmpty()) {
            return location;
        }
        return venue;
    }
    
    public String getLocationOrDisplayName() {
        if(location != null && !location.isEmpty()) {
            return location;
        }
        return getDisplayName();
    }
    
    public String getLocationAndVenue() {
        if(location != null && !location.isEmpty()) {
            return location + ", " + venue;
        }
        return venue;
    }
    
    public EventSeriesMetadataDTO getEventSeries() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.EVENT;
    }

    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return new TypeRelativeObjectIdentifier(getId().toString());
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    @Override
    public AccessControlListDTO getAccessControlList() {
        return securityInformation.getAccessControlList();
    }

    @Override
    public OwnershipDTO getOwnership() {
        return securityInformation.getOwnership();
    }

    @Override
    public void setAccessControlList(final AccessControlListDTO accessControlList) {
        securityInformation.setAccessControlList(accessControlList);
    }

    @Override
    public void setOwnership(final OwnershipDTO ownership) {
        securityInformation.setOwnership(ownership);
    }
}
