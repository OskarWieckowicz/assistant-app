package com.assistantapp.countrymcp.client;

public class RestCountriesClientException extends RuntimeException {

    public RestCountriesClientException(String message) {
        super(message);
    }

    public RestCountriesClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
