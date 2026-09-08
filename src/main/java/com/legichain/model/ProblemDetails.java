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
        @JsonProperty("errors")   List<Map<String, Object>> errors,
        /** Set on a 421 (REG_001_WRONG_REGION): the region that owns
         *  this account. The client re-pins to its host and retries,
         *  so callers rarely see this error at all. */
        @JsonProperty("region")       String region,
        /** The host that serves {@code region}. */
        @JsonProperty("api_base_url") String apiBaseUrl
) {}
