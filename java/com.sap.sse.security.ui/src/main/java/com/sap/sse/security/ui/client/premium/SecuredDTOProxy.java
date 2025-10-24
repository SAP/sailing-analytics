package com.sap.sse.security.ui.client.premium;

import com.sap.sse.security.shared.dto.SecuredDTO;

/**
 * At the point of creation of a setting, the securedDTO is not always known. Hence this class is a proxy to access a potential @SecuredDTO.
 *
 */
public class SecuredDTOProxy {
    private SecuredDTO securedDTO;
    
    public SecuredDTOProxy() {
    }

    public SecuredDTOProxy(SecuredDTO securedDTO) {
        this.setSecuredDTO(securedDTO);
    }

    public SecuredDTO getSecuredDTO() {
        return this.securedDTO;
    }
    
    public void setSecuredDTO(SecuredDTO securedDTO) {
        this.securedDTO = securedDTO;
    }

    public boolean isPresent() {
        return this.securedDTO != null;
    }
}
