package dev.danielbodi.thesis.measurement.evaluation.model;

import lombok.Getter;

/**
 * @author danielbodi
 */
@Getter
public enum Invariant {

    // I1: every payment request reached the PSP
    NO_LOST_REQUEST("I1"),

    // I2: every completed payment settled its subscription
    NO_LOST_OUTCOME("I2"),

    // I3: at most one charge per reference
    NO_DUPLICATE_CHARGE("I3");

    // the notation of the thesis
    private final String thesisId;

    Invariant(String thesisId) {
        this.thesisId = thesisId;
    }
}
