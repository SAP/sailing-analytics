package com.sap.sailing.server.gateway.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItem;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.common.fileupload.FileUploadConstants;
import com.sap.sse.filestorage.InvalidPropertiesException;
import com.sap.sse.filestorage.OperationFailedException;
import com.sap.sse.security.SecurityService;

/**
 * Accepts a multi-part MIME encoded set of files. Returns an array of JSONObjects that each contain
 * a "file_uri" value, in the same order in which the parts were sent.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class FileUploadServlet extends AbstractFileUploadServlet {
    private static final long serialVersionUID = -9002541098579359029L;

    private static final Logger logger = Logger.getLogger(FileUploadServlet.class.getName());

    /**
     * The maximum size of an image uploaded by a user as a team image, in megabytes (1024*1024 bytes).
     * Currently set to 5GB. This allows for malware scanning by services such as AWS GuardDuty.
     */
    private static final long MAX_SIZE_IN_MB = 5000;

    @Override
    protected void process(List<FileItem> fileItems, HttpServletRequest req, HttpServletResponse resp) throws UnsupportedEncodingException, IOException {
        final JSONArray resultList = new JSONArray();
        final SecurityService securityService = getSecurityService();
        if (securityService.getCurrentUser() == null) {
            final JSONObject noUserError = new JSONObject();
            noUserError.put(FileUploadConstants.STATUS, Status.FORBIDDEN.name());
            noUserError.put(FileUploadConstants.MESSAGE, "Must be authenticated to upload file");
            resultList.add(noUserError);
        } else if (!securityService.getCurrentUser().isEmailValidated()) {
            final JSONObject noVaidatedEmailAddressError = new JSONObject();
            noVaidatedEmailAddressError.put(FileUploadConstants.STATUS, Status.FORBIDDEN.name());
            noVaidatedEmailAddressError.put(FileUploadConstants.MESSAGE, "File upload permitted only with validated e-mail address");
            resultList.add(noVaidatedEmailAddressError);
        }
        /**
         * Expects the HTTP header {@code Content-Length} to be set.
         */
        for (FileItem fileItem : fileItems) {
            final JSONObject result = new JSONObject();
            final String fileExtension;
            final String fileName = Paths.get(fileItem.getName()).getFileName().toString();
            // special handling of double underscore in JSON. Double underscores will be encoded with hex representation.
            // In some cases the JSON parser of Apples Safari on mobile devices cannot parse JSON with __. See also bug5127
            // Disabled for testing the real reason of error: 
            //    String fileNameUnderscoreEncoded = fileName.replace("__", "%5f%5f");
            String fileNameUnderscoreEncoded = fileName;
            final String fileType = fileItem.getContentType();
            if (fileType.equals("image/jpeg")) {
                fileExtension = ".jpg";
            } else if (fileType.equals("image/png")) {
                fileExtension = ".png";
            } else if (fileType.equals("image/gif")) {
                fileExtension = ".gif";
            } else {
                int lastDot = fileName.lastIndexOf(".");
                if (lastDot > 0) {
                    fileExtension = fileName.substring(lastDot).toLowerCase();
                } else {
                    fileExtension = "";
                }
            }
            try {
                if (fileItem.getSize() > 1024l * 1024l * MAX_SIZE_IN_MB) {
                    final String errorMessage = "Image is larger than " + MAX_SIZE_IN_MB + "MB";
                    logger.warning("Ignoring file storage request because file " + fileName + " is larger than "
                            + MAX_SIZE_IN_MB + "MB");
                    result.put(FileUploadConstants.STATUS, Status.INTERNAL_SERVER_ERROR.name());
                    result.put(FileUploadConstants.MESSAGE, errorMessage);
                    result.put(FileUploadConstants.FILE_SIZE, fileItem.getSize());
                } else {
                    final URI fileUri = getService().getFileStorageManagementService().getActiveFileStorageService()
                            .storeFile(fileItem.getInputStream(), fileExtension, fileItem.getSize());
                    logger.info("User "+securityService.getCurrentUser().getName()+" uploaded file "+fileName+", URI "+fileUri);
                    result.put(FileUploadConstants.FILE_NAME, fileNameUnderscoreEncoded);
                    result.put(FileUploadConstants.FILE_URI, fileUri.toString());
                    result.put(FileUploadConstants.CONTENT_TYPE, fileItem.getContentType());
                    result.put(FileUploadConstants.FILE_SIZE, fileItem.getSize());
                    if (fileItem.getContentType() != null && fileItem.getContentType().startsWith("image/")) {
                        try (InputStream inputStream = fileItem.getInputStream()) {
                            BufferedImage image = ImageIO.read(inputStream);
                            if (image != null) {
                                int height = image.getHeight();
                                int width = image.getWidth();
                                result.put(FileUploadConstants.MEDIA_TYPE_HEIGHT, height);
                                result.put(FileUploadConstants.MEDIA_TYPE_WIDTH, width);
                            }
                        }
                    }
                }
            } catch (IOException | OperationFailedException | RuntimeException | InvalidPropertiesException e) {
                final String errorMessage = "Could not store file"+ (e.getMessage()==null?"":(": " + e.getMessage()));
                logger.log(Level.WARNING, "Could not store file", e);
                result.put(FileUploadConstants.STATUS, Status.INTERNAL_SERVER_ERROR.name());
                result.put(FileUploadConstants.MESSAGE, errorMessage);
            }
            resultList.add(result);
        }
        writeJsonIntoHtmlResponse(resp, resultList);
    }
}
