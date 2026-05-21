package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** One OpenSanctions / OFAC / Chainabuse match. */
public record Hit(
        @JsonProperty("entity_id")        String entityId,
        @JsonProperty("canonical_id")     String canonicalId,
        @JsonProperty("schema")           String schema,
        @JsonProperty("caption")          String caption,
        @JsonProperty("score")            int score,
        @JsonProperty("match_signals")    List<String> matchSignals,
        @JsonProperty("topics")           List<String> topics,
        @JsonProperty("sources")          List<String> sources,
        @JsonProperty("countries")        List<String> countries,
        @JsonProperty("risk_score")       int riskScore,
        @JsonProperty("risk_source")      String riskSource,
        @JsonProperty("degree")           int degree,
        @JsonProperty("match_confidence") int matchConfidence,
        @JsonProperty("name_ratio")       int nameRatio,
        @JsonProperty("flags")            HitFlags flags,
        @JsonProperty("risk_level")       RiskLevel riskLevel
) {}
