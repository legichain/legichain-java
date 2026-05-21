package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** RFC 7807 Problem Details body returned on every non-2xx response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemDetails(
        @JsonProperty("type")     String type,
        @JsonProperty("title")    String title,
        @JsonProperty("status")   int status,
        @JsonProperty("detail")   String detail,
        @JsonProperty("code")     String code,
        @JsonProperty("instance") String instance,
        @JsonProperty("errors")   List<Map<String, Object>> errors
) {}
