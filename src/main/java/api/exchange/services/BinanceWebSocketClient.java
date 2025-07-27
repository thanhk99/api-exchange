// package api.exchange.services;

// import org.java_websocket.client.WebSocketClient;
// import org.java_websocket.handshake.ServerHandshake;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import java.net.URI;

// public class BinanceWebSocketClient extends WebSocketClient {

// private final SimpMessagingTemplate messagingTemplate;

// public BinanceWebSocketClient(URI uri, SimpMessagingTemplate
// messagingTemplate) {
// // super(uri);
// // this.messagingTemplate = messagingTemplate;
// }

// @Override
// public void onOpen(ServerHandshake handshake) {
// // this.send("""
// // {
// // "method": "SUBSCRIBE",
// // "params": ["btcusdt@ticker", "ethusdt@ticker", "solusdt@ticker"],
// // "id": 1
// // }
// // """);
// }

// @Override
// public void onMessage(String message) {
// messagingTemplate.convertAndSend("/topic/prices", message);
// }

// @Override
// public void onClose(int code, String reason, boolean remote) {
// System.out.println("Closed: " + reason);
// }

// @Override
// public void onError(Exception ex) {
// ex.printStackTrace();
// }
// }