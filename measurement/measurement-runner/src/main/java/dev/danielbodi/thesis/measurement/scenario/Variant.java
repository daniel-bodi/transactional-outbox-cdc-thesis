package dev.danielbodi.thesis.measurement.scenario;

/**
 * @author danielbodi
 */
public enum Variant {

    V0, // naive dual-write
    V1; // transactional outbox + cdc

    public boolean usesConnectors() {
        return this == V1;
    }
}
