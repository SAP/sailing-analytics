package com.sap.sailing.server.gateway.trackfiles.impl;

import java.util.Arrays;

import com.sap.sailing.server.trackfiles.impl.ExpeditionImportFileHandler;

class ExpeditionImportFilenameUtils {
    private static final Iterable<String> supportedExpeditionArchiveFileExtensions = Arrays.asList("gz", "zip");

    static String truncateFilenameExtentions(String filenameWithExtensions, ExpeditionImportFileHandler fileHandler) {
        return removeTailIfExists(removeTailIfExists(filenameWithExtensions, supportedExpeditionArchiveFileExtensions),
                fileHandler.getSupportedFileExtensions());
    }

    private static String removeTailIfExists(String source, Iterable<String> extensionsToRemove) {
        for (String toRemove : extensionsToRemove) {
            final String tailToRemove = "." + toRemove;
            if (source.toLowerCase().endsWith(tailToRemove)) {
                return source.substring(0, source.length() - tailToRemove.length());
            }
        }
        return source;
    }

}
