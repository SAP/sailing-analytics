package com.sap.sailing.server.gateway.jaxrs.exceptions;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringEscapeUtils;

import com.sap.sailing.domain.common.NotFoundException;
import com.sap.sse.common.ObjectAlreadyExistsException;

@Provider
public class ExceptionManager implements ExceptionMapper<Exception> {
    public Response toResponse(final Exception exception) {
        final Response result;
        try {
            throw exception; 
        } catch (NotFoundException nfe) {
            result = Response.status(Status.NOT_FOUND).entity(nfe.getMessage()).type("text/plain").build();
        } catch (final ObjectAlreadyExistsException e) {
            result = Response.status(Status.CONFLICT).entity(e.getMessage()).type("text/plain").build();
        } catch (Exception e) {
            result = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).type("text/plain").build();
        }
        return result;
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
        return "Please provide at least one of the following parameters: "
                + Arrays.stream(params).collect(Collectors.joining(", "));
    }

    public static String invalidIdFormatMsg(String id) {
        return "The format of the id " + StringEscapeUtils.escapeHtml(id) + " is incorrect.";
    }

    public static String objectNotFoundMsg(String name, Object id) {
        return "The " + name + " with id " + id + " was not found.";
    }

    public static String incorrectParameterValue(String value, String correctValues) {
        return "The value \"" + StringEscapeUtils.escapeHtml(value) + "\" is not recognized. Correct values are: "
                + correctValues;
    }

    public static String objectAlreadyExists(String objectTypeName, String objectName) {
        return "The \"" + StringEscapeUtils.escapeHtml(objectTypeName) + "\" with name \""
                + StringEscapeUtils.escapeHtml(objectName) + "\" already exists.";
    }
}
