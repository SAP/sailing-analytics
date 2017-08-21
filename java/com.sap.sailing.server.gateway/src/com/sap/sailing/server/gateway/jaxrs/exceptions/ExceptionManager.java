package com.sap.sailing.server.gateway.jaxrs.exceptions;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringEscapeUtils;

    @Provider
    public class ExceptionManager implements ExceptionMapper<Exception>{
        public Response toResponse(Exception exception) {
            return Response.status(400).entity(exception.getMessage()).type("text/plain").build();
        }
        
        public static String invalidDateFormatMsg(String date) {
            return "The date " + StringEscapeUtils.escapeHtml(date) + " does not follow the date pattern \"dd-MM-yyyy\".";
        }
        
        public static String invalidURLFormatMsg(String url) {
            return "The format of the url " + StringEscapeUtils.escapeHtml(url) + " is incorrect.";
        }
        
        public static String parameterRequiredMsg(String param) {
            return "The parameter \"" + StringEscapeUtils.escapeHtml(param) + "\" is required.";
        }
        
        public static String atLeastOneParameterRequiredMsg(String... params) {
            return "Please provide at least one of the following parameters: "+Arrays.stream(params).collect(Collectors.joining(", "));
        }
        
        public static String invalidIdFormatMsg(String id){
            return "The format of the id " + StringEscapeUtils.escapeHtml(id) + " is incorrect.";
        }
        
        public static String objectNotFoundMsg(String name, UUID id){
            return "The "+name+" with id " + id + " was not found.";
        }
        
        public static String incorrectParameterValue(String value, String correctValues){
            return "The value \"" + StringEscapeUtils.escapeHtml(value) +"\" is not recognized. Correct values are: " + correctValues;
        }
    } 


