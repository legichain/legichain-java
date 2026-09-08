package com.legichain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Durable receipt; status may still be queued or running. */
public record OperationAccepted(
 @JsonProperty("operation_id") String operationId,
 @JsonProperty("status") String status,
 @JsonProperty("status_url") String statusUrl,
 @JsonProperty("deadline_at") String deadlineAt,
 @JsonProperty("protocol") int protocol,
 @JsonProperty("items") Integer items
) {}
