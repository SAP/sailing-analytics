package com.sap.sailing.polars.jaxrs.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.polars.ReplicablePolarService;

public class FileBasedPolarDataClient extends PolarDataClient {

    private final File file;

    public FileBasedPolarDataClient(File file, ReplicablePolarService polarService, DomainFactory domainFactory) {
        super(null, polarService, Optional.empty());
        polarService.registerDomainFactory(domainFactory);
        this.file = file;
    }

    @Override
    protected InputStream getContentFromResponse() throws IOException, ParseException {
        return new FileInputStream(file);
    }
}
