package com.sap.sse.security.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.security.shared.dto.UserDTO;

public class SuccessInfo implements IsSerializable {

    public static final long serialVersionUID = -3044914225885460520L;

    public static final String FAILED_TO_LOGIN = "Failed to login.";
    
    private boolean successful;
    private String message;
    private Triple<UserDTO, UserDTO, ServerInfoDTO> userDTO;
    private String redirectURL;
    private String extra;
    
    SuccessInfo() {} // for serializtion only
    
    public SuccessInfo(boolean successful, String message, String redirectURL, Triple<UserDTO, UserDTO, ServerInfoDTO> userDTO) {
        super();
        this.successful = successful;
        this.message = message;
        this.redirectURL = redirectURL;
        this.userDTO = userDTO;
    }
    
    public SuccessInfo(boolean successful, String message, String redirectURL, Triple<UserDTO, UserDTO, ServerInfoDTO> userDTO, String extra) {
        super();
        this.successful = successful;
        this.message = message;
        this.redirectURL = redirectURL;
        this.userDTO = userDTO;
        this.extra = extra;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public Triple<UserDTO, UserDTO, ServerInfoDTO> getUserDTO() {
        return userDTO;
    }
    
    public String getRedirectURL() {
        return redirectURL;
    }

    public String getExtra() {
        return extra;
    }
}
