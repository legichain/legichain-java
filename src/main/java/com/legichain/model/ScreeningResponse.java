package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Canonical screen response. */
public record ScreeningResponse(
        @JsonProperty("request_id")        String requestId,
        @JsonProperty("matched")           boolean matched,
        @JsonProperty("summary")           ScreeningSummary summary,
        @JsonProperty("hits")              List<Hit> hits,
        @JsonProperty("search_time_ms")    int searchTimeMs,
        @JsonProperty("cost_credits")      int costCredits,
        @JsonProperty("credits_remaining") int creditsRemaining,
        @JsonProperty("screening_id")      String screeningId
) {}
