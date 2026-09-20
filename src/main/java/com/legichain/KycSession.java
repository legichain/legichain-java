package com.legichain;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.legichain.model.OperationAccepted;

/** One applicant; creation and internal client-token management are handled by the SDK. */
public final class KycSession {
    private final Legichain client;
    private final String applicationId;
    private final String token;
    private final Set<String> operations = ConcurrentHashMap.newKeySet();
    KycSession(Legichain client, String applicationId, String token) {
        this.client=client;this.applicationId=applicationId;this.token=token;
    }
    public String applicationId() { return applicationId; }
    public OperationAccepted evidence(String step, Object body, String idempotencyKey) {
        var receipt=client.enqueueKycEvidence(applicationId,step,body,idempotencyKey,token);
        operations.add(receipt.operationId());return receipt;
    }
    public Map<String,Object> challenge() { return client.kycLivenessChallenge(applicationId,token,3,120); }
    public Map<String,Object> status() { return client.kycStatus(applicationId); }
    public JsonNode awaitEvidence(String id, Duration timeout) throws InterruptedException, TimeoutException {
        if(!operations.contains(id)) throw new IllegalArgumentException("Operation does not belong to this SDK session");
        if(timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("Timeout must be positive");
        long deadline=System.nanoTime()+timeout.toNanos();
        while(true) {
            if(Thread.currentThread().isInterrupted()) throw new InterruptedException();
            var result=client.operation(id);var state=result.path("status").asText();
            if(state.equals("completed")) return result;
            if(Set.of("failed","expired","cancelled").contains(state)) throw new IllegalStateException("KYC operation "+id+" "+state+": "+result.path("error_code").asText());
            long remaining=deadline-System.nanoTime();
            if(remaining<=0) throw new TimeoutException("Evidence is still processing; keep the operation receipt");
            Thread.sleep(Math.max(1,Math.min(1000,remaining/1_000_000)));
        }
    }
    /** Final identity decisions are delivered by webhook; this records submission. */
    public Map<String,Object> submit() {
        for(String id:operations) if(!client.operation(id).path("status").asText().equals("completed"))
            throw new IllegalStateException("Evidence is not complete; do not submit yet");
        return client.kycSubmit(applicationId,token);
    }
}
