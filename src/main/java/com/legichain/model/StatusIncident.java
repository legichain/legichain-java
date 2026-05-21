package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatusIncident(
        @JsonProperty("id")          String id,
        @JsonProperty("title")       String title,
        @JsonProperty("severity")    String severity,
        @JsonProperty("status")      String status,
        @JsonProperty("started_at")  String startedAt,
        @JsonProperty("resolved_at") String resolvedAt,
        @JsonProperty("description") String description
) {}
