package dev.danielbodi.thesis.measurement.evaluation.model;

import lombok.Getter;

/**
 * @author danielbodi
 */
@Getter
public enum Invariant {

    // I1(a): every payment request reached the PSP
    NO_LOST_REQUEST("I1(a)"),

    // I1(b): every completed payment settled its subscription
    NO_LOST_OUTCOME("I1(b)"),

    // I2: at most one charge per reference
    NO_DUPLICATE_CHARGE("I2"),

    // I3: no charge without a committed payment request
    NO_PHANTOM_CHARGE("I3");

    // the notation of the thesis
    private final String thesisId;

    Invariant(String thesisId) {
        this.thesisId = thesisId;
    }
}
