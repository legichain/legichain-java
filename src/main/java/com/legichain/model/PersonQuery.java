package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** POST /v1/screen/person body. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonQuery(
        @JsonProperty("name")     String name,
        @JsonProperty("country")  String country,
        @JsonProperty("dob")      String dob,
        @JsonProperty("document") String document,
        @JsonProperty("topics")   List<String> topics,
        @JsonProperty("top_n")    Integer topN
) {
    public PersonQuery(String name) { this(name, null, null, null, null, null); }
}
