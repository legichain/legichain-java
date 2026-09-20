package com.legichain;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KycSessionTest {
 @Test void preservesCredentialsAndDefersSubmission() throws Exception {
  var state=new AtomicReference<>("running");var submissions=new AtomicInteger();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",e->{
   String path=e.getRequestURI().getPath();String body;
   if(path.equals("/v1/kyc/applications")) body="{\"application_id\":\"tr_app\",\"client_token\":\"internal\"}";
   else if(path.equals("/v2/operations/tr_op")) body="{\"status\":\""+state.get()+"\"}";
   else if(path.endsWith("/submit")) {submissions.incrementAndGet();body="{\"pending\":true,\"outcome\":null,\"state\":\"deciding\"}";}
   else {assertEquals("internal",e.getRequestHeaders().getFirst("X-KYC-Client-Token"));assertEquals("capture",e.getRequestHeaders().getFirst("Idempotency-Key"));body="{\"operation_id\":\"tr_op\",\"status\":\"queued\",\"protocol\":1}";}
   byte[] bytes=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(202,bytes.length);e.getResponseBody().write(bytes);e.close();
  });server.start();
  try {
   var client=Legichain.builder().apiKey("synthetic").baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).build();
   var session=client.startKyc(Map.of("liveness_required",false),"create");
   var receipt=session.evidence("liveness",Map.of("mode","passive","frame_b64","test","frame_mime_type","image/jpeg"),"capture");
   assertThrows(IllegalStateException.class,session::submit);assertEquals(0,submissions.get());
   state.set("failed");assertThrows(IllegalStateException.class,()->session.awaitEvidence(receipt.operationId(),Duration.ofSeconds(1)));
   state.set("completed");assertEquals(true,session.submit().get("pending"));assertEquals(1,submissions.get());
  } finally {server.stop(0);}
 }
}
