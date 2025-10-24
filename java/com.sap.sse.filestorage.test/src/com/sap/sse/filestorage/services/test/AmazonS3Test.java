package com.sap.sse.filestorage.services.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sse.filestorage.FileStorageService;
import com.sap.sse.filestorage.InvalidPropertiesException;
import com.sap.sse.filestorage.OperationFailedException;
import com.sap.sse.filestorage.testsupport.AmazonS3TestSupport;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;

public class AmazonS3Test {
    @BeforeEach
    public void setup() throws InvalidPropertiesException, IOException {
        setUpSecurity();
        storageService = AmazonS3TestSupport.createService(SecurityServiceMockFactory.mockSecurityService());
    }

    private void setUpSecurity() {
        org.apache.shiro.mgt.SecurityManager securityManager = Mockito.mock(org.apache.shiro.mgt.SecurityManager.class);
        Subject fakeSubject = Mockito.mock(Subject.class);
        SecurityUtils.setSecurityManager(securityManager);
        Mockito.doReturn(fakeSubject).when(securityManager).createSubject(Mockito.any());
        Mockito.doReturn(true).when(fakeSubject).isAuthenticated();
        Mockito.doNothing().when(fakeSubject).checkPermission(Mockito.anyString());
    }

    private static final String teamImageFile = "Bundesliga2014_Regatta6_eventteaser.jpg";
    private FileStorageService storageService;
    
    @Test
    public void testStoreAndRemoveFileTest() throws URISyntaxException, IOException, OperationFailedException, InvalidPropertiesException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(teamImageFile);
        // this is not ideal, as this #available() is not supposed to be used for getting the file size
        // however, working with a File() descriptor does not work, as when running via maven/tycho the
        // URL has the bundleresource:// scheme instead of file:, which File() can't handle
        long length = stream.available();
        URI uri = storageService.storeFile(stream, teamImageFile, length);
        InputStream downloadStream = uri.toURL().openStream();
        stream = getClass().getClassLoader().getResourceAsStream(teamImageFile);
        try {
            assertTrue(IOUtils.contentEquals(downloadStream, stream));
        } finally {
            storageService.removeFile(uri);
        }
    }
}
