package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** One-shot verdict the API surfaces for every screening response. */
public enum Recommendation {
    CLEAR("clear"),
    REVIEW("review"),
    BLOCK("block");

    private final String wire;

    Recommendation(String wire) { this.wire = wire; }

    @JsonValue public String wire() { return wire; }

    @JsonCreator
    public static Recommendation fromWire(String v) {
        if (v == null) return CLEAR;
        for (Recommendation r : values()) if (r.wire.equalsIgnoreCase(v)) return r;
        return CLEAR;
    }
}
