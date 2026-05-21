package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** POST /v1/screen/company body. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyQuery(
        @JsonProperty("name")                String name,
        @JsonProperty("country")             String country,
        @JsonProperty("registration_number") String registrationNumber,
        @JsonProperty("top_n")               Integer topN
) {
    public CompanyQuery(String name) { this(name, null, null, null); }
}
