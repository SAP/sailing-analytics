package com.sap.sse.filestorage.impl;

import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;

public class FileStorageI18n {    
    private static final String RESOURCE_BASE_NAME = "stringmessages/FileStorageStringMessages";
    
    public static final ResourceBundleStringMessages STRING_MESSAGES = new ResourceBundleStringMessagesImpl(RESOURCE_BASE_NAME, FileStorageI18n.class.getClassLoader());
}
