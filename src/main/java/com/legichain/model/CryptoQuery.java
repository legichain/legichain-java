package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** POST /v1/screen/crypto body. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CryptoQuery(
        @JsonProperty("address") String address,
        @JsonProperty("chain")   String chain
) {
    public CryptoQuery(String address) { this(address, null); }
}
