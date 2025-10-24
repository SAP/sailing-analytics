package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.SecurityInformationDTO;

public class IgtimiDataAccessWindowWithSecurityDTO implements SecuredDTO {
    private static final long serialVersionUID = 176992188692729118L;
    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();

    private long id;
    private String deviceSerialNumber;
    private Date from;
    private Date to;
    
    @Deprecated // GWT serialization only
    IgtimiDataAccessWindowWithSecurityDTO() {}
    
    public IgtimiDataAccessWindowWithSecurityDTO(long id, String deviceSerialNumber, Date from, Date to) {
        this.id = id;
        this.deviceSerialNumber = deviceSerialNumber;
        this.from = from;
        this.to = to;
    }

    public SecurityInformationDTO getSecurityInformation() {
        return securityInformation;
    }

    public long getId() {
        return id;
    }

    public String getSerialNumber() {
        return deviceSerialNumber;
    }

    public Date getFrom() {
        return from;
    }
    
    public Date getTo() {
        return to;
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
    public void setAccessControlList(AccessControlListDTO accessControlList) {
        securityInformation.setAccessControlList(accessControlList);
    }

    @Override
    public void setOwnership(OwnershipDTO ownership) {
        securityInformation.setOwnership(ownership);
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.IGTIMI_DATA_ACCESS_WINDOW;
    }

    private TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        // TODO it would be nice to factor the redundancy with DataAccessWindow.getTypeRelativeObjectIdentifier but it
        // would require introducing a new .common bundle
        final TimePoint startTime = TimePoint.of(getFrom());
        final TimePoint endTime = TimePoint.of(getTo());
        return new TypeRelativeObjectIdentifier(getSerialNumber(),
                "" + (startTime == null ? "null" : startTime.asMillis()),
                "" + (endTime == null ? "null" : endTime.asMillis()));
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    @Override
    public String getName() {
        return "DataAccessWindow "+getId();
    }
}
