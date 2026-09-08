package com.legichain;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperationsTest {
 @Test void explicitReceiptKeysAndStatus() throws Exception {
  var paths=new ArrayList<String>();var keys=new ArrayList<String>();var tokens=new ArrayList<String>();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",exchange->{
   paths.add(exchange.getRequestURI().getPath());keys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));tokens.add(exchange.getRequestHeaders().getFirst("X-KYC-Client-Token"));
   var bytes="{\"operation_id\":\"tr_op\",\"status\":\"queued\",\"status_url\":\"/v2/operations/tr_op\",\"deadline_at\":\"2026-09-07T00:00:00Z\",\"protocol\":1}".getBytes(StandardCharsets.UTF_8);
   exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(202,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
  });server.start();
  try {
   var lc=Legichain.builder().apiKey("synthetic.key").baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).build();
   for(int i=0;i<2;i++)assertEquals("queued",lc.enqueueScreen("person",Map.of("name","Synthetic"),"stable-key").status());
   assertEquals(2,paths.size());assertEquals("stable-key",keys.get(0));assertEquals(keys.get(0),keys.get(1));
   lc.enqueueKycEvidence("tr_app","nfc",Map.of("access_error","synthetic"),"nfc","token");assertEquals("token",tokens.get(2));
   lc.enqueueReport("wallet",Map.of(),"report");lc.enqueueKycReport("tr_app",Map.of(),"kyc-report");lc.enqueueAddressSubmit("tr_av",Map.of(),"av");
   assertEquals("queued",lc.operation("tr_op").get("status").asText());lc.operationTask("tr_op","task");lc.operations(null,"failed",25);lc.cancelOperation("tr_op");
   int count=paths.size();
   assertThrows(IllegalArgumentException.class,()->lc.enqueueScreen("person",Map.of(),""));
   assertThrows(IllegalArgumentException.class,()->lc.enqueueScreen("../reports",Map.of(),"key"));assertEquals(count,paths.size());
  } finally {server.stop(0);}
 }
}
