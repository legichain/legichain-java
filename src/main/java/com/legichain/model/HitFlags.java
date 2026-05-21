package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Boolean category flags expanded from the per-hit topic list — banks
 *  can branch on these without parsing free-text topic codes. */
public record HitFlags(
        @JsonProperty("is_sanctioned")    boolean isSanctioned,
        @JsonProperty("is_pep")           boolean isPep,
        @JsonProperty("is_wanted")        boolean isWanted,
        @JsonProperty("is_crime")         boolean isCrime,
        @JsonProperty("is_adverse_media") boolean isAdverseMedia
) {}
