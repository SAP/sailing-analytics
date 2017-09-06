package com.sap.sailing.server.gateway.test.jaxrs;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;
import com.sap.sailing.server.gateway.jaxrs.api.CompetitorsResource;
import com.sap.sse.filestorage.FileStorageManagementService;
import com.sap.sse.filestorage.FileStorageService;
import com.sap.sse.filestorage.InvalidPropertiesException;
import com.sap.sse.filestorage.OperationFailedException;
import com.sap.sse.filestorage.testsupport.AmazonS3TestSupport;

public class CompetitorsResourceTeamImageTest extends AbstractJaxRsApiTest {    
    private static final String name = "Heiko KRÖGER";
    private static final String id = "af855a56-9726-4a9c-a77e-da955bd289be";
    private static final String boatClassName = "49er";
    private static final String sailID = "GER 1";
    private static final String nationality = "GER";
    private static final String teamImageFile = "Bundesliga2014_Regatta6_eventteaser.jpg";
    
    private FileStorageService storageService;

    @Before
    public void setUpSubClass() throws Exception {
        super.setUp();
        racingEventService = spy(racingEventService);
        storageService = AmazonS3TestSupport.createService();
        FileStorageManagementService fsmsMock = mock(FileStorageManagementService.class);
        doReturn(fsmsMock).when(racingEventService).getFileStorageManagementService();
        doReturn(storageService).when(fsmsMock).getActiveFileStorageService();
        DynamicTeam team = new TeamImpl(null, Collections.singleton(new PersonImpl(null, new NationalityImpl(
                nationality), null, null)), null);
        DynamicBoat boat = new BoatImpl(null, new BoatClassImpl(boatClassName, false), sailID);
        racingEventService.getBaseDomainFactory().getOrCreateCompetitor(id, name, null, null, null, team, boat,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
    }

    @Test
    public void storeAndRemoveTeamImage() throws URISyntaxException, ParseException, MalformedURLException,
            IOException, OperationFailedException, InvalidPropertiesException {
        //set team image
        CompetitorsResource r = spyResource(new CompetitorsResource());
        String fileExtension = teamImageFile.substring(teamImageFile.lastIndexOf("."));;
        InputStream stream = getClass().getResourceAsStream("/" + teamImageFile);
        
        // this is not ideal, as this #available() is not supposed to be used for getting the file size
        // however, working with a File() descriptor does not work, as when running via maven/tycho the
        // URL has the bundleresource:// scheme instead of file:, which File() can't handle
        long length = stream.available();
        
        String jsonString = r.setTeamImage(id, stream, fileExtension, length);
        
        //now download and compare
        JSONObject json = (JSONObject) JSONValue.parseWithException(jsonString);
        String imageUriString = (String) json.get(DeviceMappingConstants.JSON_TEAM_IMAGE_URI);
        URI imageUri = new URI(imageUriString);
        
        InputStream downloadStream = imageUri.toURL().openStream();
        stream = getClass().getResourceAsStream("/" + teamImageFile);
        IOUtils.contentEquals(downloadStream, stream);
        
        storageService.removeFile(imageUri);
    }
}
