package com.sap.sailing.landscape.impl;

import com.sap.sailing.landscape.AwsSessionCredentialsWithExpiry;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.LongSetting;
import com.sap.sse.common.settings.generic.StringSetting;

public class AwsSessionCredentialsFromUserPreference extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -3250243915670349222L;

    private StringSetting accessKeyId;
    private StringSetting secretAccessKey;
    private StringSetting sessionToken;
    private LongSetting expiry;
    
    @Override
    protected void addChildSettings() {
        accessKeyId = new StringSetting("accessKeyId", this);
        secretAccessKey = new StringSetting("secretAccessKey", this);
        sessionToken = new StringSetting("sessionToken", this);
        expiry = new LongSetting("expiry", this);
    }

    /**
     * The default settings
     */
    public AwsSessionCredentialsFromUserPreference() {
        super();
    }

    public AwsSessionCredentialsFromUserPreference(AwsSessionCredentialsWithExpiry awsSessionCredentialsWithExpiry) {
        this();
        this.accessKeyId.setValue(awsSessionCredentialsWithExpiry.getAccessKeyId());
        this.secretAccessKey.setValue(awsSessionCredentialsWithExpiry.getSecretAccessKey());
        this.sessionToken.setValue(awsSessionCredentialsWithExpiry.getSessionToken());
        this.expiry.setValue(awsSessionCredentialsWithExpiry.getExpiration().asMillis());
    }

    public AwsSessionCredentialsWithExpiry getAwsSessionCredentialsWithExpiry() {
        return new AwsSessionCredentialsWithExpiryImpl(accessKeyId.getValue(), secretAccessKey.getValue(), sessionToken.getValue(), TimePoint.of(expiry.getValue()));
    }
}
