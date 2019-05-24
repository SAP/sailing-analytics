package com.sap.sailing.gwt.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.SecurityInformationDTO;

public class TracTracConfigurationWithSecurityDTO implements IsSerializable, SecuredDTO {
    private static final long serialVersionUID = -3567107321280535272L;

    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();

    private String name;
    private String jsonUrl;
    private String liveDataURI;
    private String storedDataURI;
    private String courseDesignUpdateURI;
    private String tracTracUsername;
    private String tracTracPassword;
    private String creatorName;

    public TracTracConfigurationWithSecurityDTO() {
    }
    
    public TracTracConfigurationWithSecurityDTO(String creatorName) {
        this.creatorName = creatorName;
    }

    public TracTracConfigurationWithSecurityDTO(String name, String jsonUrl, String liveDataURI, String storedDataURI,
            String courseDesignUpdateUrl, String tractracUsername, String tractracPassword, String creatorName) {
        super();
        this.name = name;
        this.jsonUrl = jsonUrl;
        this.liveDataURI = liveDataURI;
        this.storedDataURI = storedDataURI;
        this.courseDesignUpdateURI = courseDesignUpdateUrl;
        this.tracTracUsername = tractracUsername;
        this.tracTracPassword = tractracPassword;
        this.creatorName = creatorName;
    }

    /** Copy constructor with new name */
    public TracTracConfigurationWithSecurityDTO(TracTracConfigurationWithSecurityDTO config, final String name) {
        super();
        this.name = name;
        this.jsonUrl = config.getJsonUrl();
        this.liveDataURI = config.getLiveDataURI();
        this.storedDataURI = config.getStoredDataURI();
        this.courseDesignUpdateURI = config.getCourseDesignUpdateURI();
        this.tracTracUsername = config.getTracTracUsername();
        this.tracTracPassword = config.getTracTracPassword();
        this.creatorName = config.getCreatorName();
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
    public void setAccessControlList(AccessControlListDTO createAccessControlListDTO) {
        securityInformation.setAccessControlList(createAccessControlListDTO);
    }

    @Override
    public void setOwnership(OwnershipDTO createOwnershipDTO) {
        securityInformation.setOwnership(createOwnershipDTO);
    }

    public SecurityInformationDTO getSecurityInformation() {
        return securityInformation;
    }

    public String getName() {
        return name;
    }

    public String getJsonUrl() {
        return jsonUrl;
    }

    public String getLiveDataURI() {
        return liveDataURI;
    }

    public String getStoredDataURI() {
        return storedDataURI;
    }

    public String getCourseDesignUpdateURI() {
        return courseDesignUpdateURI;
    }

    public String getTracTracUsername() {
        return tracTracUsername;
    }

    public String getTracTracPassword() {
        return tracTracPassword;
    }

    public String getCreatorName() {
        return creatorName;
    }

    @Override
    public HasPermissions getType() {
        return SecuredDomainType.TRACTRAC_ACCOUNT;
    }

    private TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return creatorName == null ? new TypeRelativeObjectIdentifier(jsonUrl)
                : new TypeRelativeObjectIdentifier(jsonUrl, creatorName);
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }
}
