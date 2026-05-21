package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BatchAsyncResponse(
        @JsonProperty("job_id")         String jobId,
        @JsonProperty("status")         String status,
        @JsonProperty("items")          int items,
        @JsonProperty("callback_event") String callbackEvent
) {}
