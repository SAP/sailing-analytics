package com.sap.sailing.server.trackfiles.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.sap.sailing.domain.trackimport.FormatNotSupportedException;

public abstract class ExpeditionImportFileHandler implements CompressedStreamsUtil.FileHandler {

    public final Iterable<String> supportedExpeditionLogFileExtensions;

    public Iterable<String> getSupportedFileExtensions() {
        return supportedExpeditionLogFileExtensions;
    }
    
    protected ExpeditionImportFileHandler(Iterable<String> supportedFileExtensions) {
        this.supportedExpeditionLogFileExtensions = supportedFileExtensions;
    }
    
    protected ExpeditionImportFileHandler() {
        this(Arrays.asList("csv", "log", "txt"));
    }

    @Override
    public final void handle(String fileName, InputStream inputStream, Charset charset) throws IOException, FormatNotSupportedException {
        final String lowerCaseFileName = fileName.toLowerCase();
        boolean extensionSupported = false;
        for (String extension : supportedExpeditionLogFileExtensions) {
            if (lowerCaseFileName.endsWith(extension)) {
                extensionSupported = true;
                break;
            }
        }
        if (extensionSupported) {
            handleExpeditionFile(lowerCaseFileName, inputStream, charset);
        }
    }

    protected abstract void handleExpeditionFile(String fileName, InputStream inputStream, Charset charset) throws IOException, FormatNotSupportedException;
}
