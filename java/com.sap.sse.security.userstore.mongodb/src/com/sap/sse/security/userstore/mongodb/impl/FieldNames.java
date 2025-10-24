package com.sap.sse.security.userstore.mongodb.impl;

public class FieldNames {
    
    public static enum AccessControlList {
        OBJECT_ID,
        OBJECT_DISPLAY_NAME,
        PERMISSION_MAP, // a list of objects with two components each:
            PERMISSION_MAP_USER_GROUP_ID,
            PERMISSION_MAP_ACTIONS
    }
    
    public static enum Ownership {
        /**
         * The ID of the owned object
         */
        OBJECT_ID,
        OWNER_USERNAME,
        OBJECT_DISPLAY_NAME,
        TENANT_OWNER_ID
    }
    
    public static enum Role {
        ID,
        NAME,
        PERMISSIONS,
        TRANSITIVE,
        QUALIFYING_TENANT_ID,
        QUALIFYING_TENANT_NAME, // for human readability only
        QUALIFYING_USERNAME
    }
    
    public static enum Tenant {
        ID
    }
    
    public static enum UserGroup {
        ID,
        NAME,
        USERNAMES,
        ROLE_DEFINITION_MAP, // a list of objects with two components each:
        ROLE_DEFINITION_MAP_ROLE_ID,
        ROLE_DEFINITION_MAP_FOR_ALL
    }
    
    public static enum User {
        NAME,
        FULLNAME,
        COMPANY,
        LOCALE,
        EMAIL,
        ACCOUNTS,
        ROLE_IDS, PERMISSIONS,
        DEFAULT_TENANT_IDS,
        EMAIL_VALIDATED,
        PASSWORD_RESET_SECRET,
        VALIDATION_SECRET,
        DEFAULT_TENANT_SERVER,
        DEFAULT_TENANT_GROUP,
        SUBSCRIPTIONS,
        LOCKED_UNTIL_MILLIS,
        NEXT_LOCKING_DURATION_MILLIS;
    }
    
    public static enum Settings {
        NAME,
        MAP,
        TYPES,
        VALUES;
    }
    
    public static enum Preferences {
        USERNAME,
        KEYS_AND_VALUES,
        KEY,
        VALUE;
    }
    
    public static enum Account {
        NAME;
    }
    
    public static enum UsernamePassword {
        NAME,
        SALTED_PW,
        SALT;
    }
}
