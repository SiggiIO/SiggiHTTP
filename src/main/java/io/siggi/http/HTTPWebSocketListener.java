package io.siggi.http;

public interface HTTPWebSocketListener {
	public void receivedMessage(HTTPWebSocket socket, HTTPWebSocketMessage message);
	public void socketClosed(HTTPWebSocket socket);
}
