package com.sap.sailing.ingestion.dto;

import java.io.Serializable;

/**
 * In most cases requests coming to a lambda will be serialized into JSON. That means that all fields a HTTP requets
 * normally has can be found inside the JSON. This implementation just provides the body and the method.
 * 
 * <pre>
 * {
 *   "requestContext": {
 *       "elb": {
 *           "targetGroupArn": "arn:aws:elasticloadbalancing:us-east-2:123456789012:targetgroup/lambda-279XGJDqGZ5rsrHC2Fjr/49e9d65c45c6791a"
 *       }
 *   },
 *   "httpMethod": "GET",
 *   "path": "/lambda",
 *   "queryStringParameters": {
 *       "query": "1234ABCD"
 *   },
 *   "headers": {
 *       "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng",
 *       "accept-encoding": "gzip",
 *       "accept-language": "en-US,en;q=0.9",
 *       "connection": "keep-alive",
 *       "host": "lambda-alb-123578498.us-east-2.elb.amazonaws.com",
 *       "upgrade-insecure-requests": "1",
 *       "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36",
 *       "x-amzn-trace-id": "Root=1-5c536348-3d683b8b04734faae651f476",
 *       "x-forwarded-for": "72.12.164.125",
 *       "x-forwarded-port": "80",
 *       "x-forwarded-proto": "http",
 *       "x-imforwards": "20"
 *   },
 *   "body": "Whatever body is being transmitted",
 *   "isBase64Encoded": false
 *}
 * </pre>
 */
public class AWSRequestWrapper<T> implements Serializable {

    private static final long serialVersionUID = 6640992447269375968L;

    private String httpMethod;
    private T body;

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

}
