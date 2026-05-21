package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record StatusPayload(
        @JsonProperty("status")           String status,
        @JsonProperty("as_of")            String asOf,
        @JsonProperty("components")       Map<String, String> components,
        @JsonProperty("active_incidents") List<StatusIncident> activeIncidents,
        @JsonProperty("recent_30d")       List<StatusIncident> recent30d
) {}
