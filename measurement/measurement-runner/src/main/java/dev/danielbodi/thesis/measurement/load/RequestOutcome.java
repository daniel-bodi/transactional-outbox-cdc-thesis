package dev.danielbodi.thesis.measurement.load;

/**
 * @author danielbodi
 */
public enum RequestOutcome {
    CREATED, // HTTP 201
    REJECTED, // an HTTP response other than 201
    NO_RESPONSE // connection refused or reset, timeout
}
