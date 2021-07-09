package com.sap.sse.security.shared.dto;

/**
 * {@link NamedDTO} extension which also implements {@link SecuredObject} interface.
 */
public abstract class NamedSecuredObjectDTO extends NamedDTO implements SecuredDTO {

    private static final long serialVersionUID = 2642220699434177353L;

    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();

    @Deprecated
    protected NamedSecuredObjectDTO() {} // for GWT RPC serialization only

    protected NamedSecuredObjectDTO(String name) {
        super(name);
    }

    @Override
    public final AccessControlListDTO getAccessControlList() {
        return securityInformation.getAccessControlList();
    }

    @Override
    public final OwnershipDTO getOwnership() {
        return securityInformation.getOwnership();
    }

    @Override
    public final void setAccessControlList(final AccessControlListDTO accessControlList) {
        this.securityInformation.setAccessControlList(accessControlList);
    }

    @Override
    public final void setOwnership(final OwnershipDTO ownership) {
        this.securityInformation.setOwnership(ownership);
    }

}
