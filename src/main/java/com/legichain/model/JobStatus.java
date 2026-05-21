package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobStatus(
        @JsonProperty("job_id")      String jobId,
        @JsonProperty("status")      String status,
        @JsonProperty("kind")        String kind,
        @JsonProperty("started_at")  String startedAt,
        @JsonProperty("finished_at") String finishedAt,
        @JsonProperty("result")      Object result,
        @JsonProperty("error")       String error
) {}
