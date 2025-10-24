package com.sap.sse.filestorage.testsupport;

import java.io.IOException;

import com.sap.sse.filestorage.InvalidPropertiesException;
import com.sap.sse.filestorage.impl.AmazonS3FileStorageServiceImpl;
import com.sap.sse.filestorage.impl.BaseFileStorageServiceImpl;
import com.sap.sse.security.SecurityService;

/**
 * Provide the S3 credentials for an IAM account that has access to the "sapsailing-automatic-upload-test" bucket in the
 * system properties {@code aws.s3.test.s3AccessId} and {@code aws.s3.test.s3AccessKey}. For the build script in
 * {@code configuration/buildAndUpdateProduct.sh} this can be done by setting the {@code APP_PARAMETERS} environment
 * variable for the script like this:
 * {@code APP_PARAMETERS="-Daws.s3.test.s3AccessId=... -Daws.s3.test.s3AccessKey=..."}. Alternatively, e.g., in order to
 * avoid system property setting with the "-D" command line option to be shown in log files, you may pass the ID and key
 * as environment variables {@code AWS_S3_TEST_S3ACCESSID} and {@code AWS_S3_TEST_S3ACCESSKEY}, respectively.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class AmazonS3TestSupport {
    public static final String s3AccessId = System.getProperty("aws.s3.test.s3AccessId", System.getenv("AWS_S3_TEST_S3ACCESSID"));
    public static final String s3AccessKey = System.getProperty("aws.s3.test.s3AccessKey", System.getenv("AWS_S3_TEST_S3ACCESSKEY"));
    private static final String s3BucketName = "sapsailing-automatic-upload-test";
    
    public static BaseFileStorageServiceImpl createService(final SecurityService securityService) throws InvalidPropertiesException, IOException {
        BaseFileStorageServiceImpl service = new AmazonS3FileStorageServiceImpl(/* bundleContext */ null) {
            private static final long serialVersionUID = 6887160074291578082L;

            @Override
            protected SecurityService getSecurityService() {
                return securityService;
            }
        };
        service.internalSetProperty("accessId", s3AccessId);
        service.internalSetProperty("accessKey", s3AccessKey);
        service.internalSetProperty("bucketName", s3BucketName);
        service.testProperties();
        return service;
    }
}
