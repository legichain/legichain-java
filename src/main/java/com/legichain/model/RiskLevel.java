package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Five-tier risk vocabulary, mapped server-side from the 0..10 risk
 *  score. Matches the PDF report's "Critical / High / …" labels. */
public enum RiskLevel {
    NO("no"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");

    private final String wire;

    RiskLevel(String wire) { this.wire = wire; }

    @JsonValue
    public String wire() { return wire; }

    @JsonCreator
    public static RiskLevel fromWire(String v) {
        if (v == null) return NO;
        for (RiskLevel r : values()) if (r.wire.equalsIgnoreCase(v)) return r;
        return NO;
    }
}
