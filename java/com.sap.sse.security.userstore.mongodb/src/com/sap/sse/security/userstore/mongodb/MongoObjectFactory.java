package com.sap.sse.security.userstore.mongodb;

import java.util.Map;

import com.sap.sse.security.userstore.shared.User;

public interface MongoObjectFactory {

    public void storeUser(User user);
    
    public void deleteUser(User user);
    
    public void storeSettings(Map<String, Object> settings);
    
    public void storeSettingTypes(Map<String, Class<?>> settingTypes);
}
