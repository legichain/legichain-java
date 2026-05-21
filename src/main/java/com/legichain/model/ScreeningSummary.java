package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Aggregate verdict across all hits. Branch on {@link #recommendation()}. */
public record ScreeningSummary(
        @JsonProperty("matched")               boolean matched,
        @JsonProperty("hit_count")             int hitCount,
        @JsonProperty("has_sanctioned_hit")    boolean hasSanctionedHit,
        @JsonProperty("has_pep_hit")           boolean hasPepHit,
        @JsonProperty("has_wanted_hit")        boolean hasWantedHit,
        @JsonProperty("has_crime_hit")         boolean hasCrimeHit,
        @JsonProperty("has_adverse_media_hit") boolean hasAdverseMediaHit,
        @JsonProperty("top_risk_score")        int topRiskScore,
        @JsonProperty("top_risk_level")        RiskLevel topRiskLevel,
        @JsonProperty("top_match_confidence")  int topMatchConfidence,
        @JsonProperty("recommendation")        Recommendation recommendation,
        @JsonProperty("authorities")           List<String> authorities,
        @JsonProperty("sources")               List<String> sources
) {}
