package com.example.mapslocalization.errors;


/**
 * Indicates that the API call was not configured for the supplied credentials and environmental
 * conditions. Check the error message for details.
 */
public class AccessNotConfiguredException extends ApiException {

    private static final long serialVersionUID = -9167434506751721386L;

    public AccessNotConfiguredException(String errorMessage) {
        super(errorMessage);
    }
}