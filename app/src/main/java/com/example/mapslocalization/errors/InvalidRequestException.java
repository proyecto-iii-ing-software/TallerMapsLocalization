package com.example.mapslocalization.errors;

/** Indicates that the API received a malformed request. */
public class InvalidRequestException extends ApiException {

    private static final long serialVersionUID = -5682669561780594333L;

    public InvalidRequestException(String errorMessage) {
        super(errorMessage);
    }
}