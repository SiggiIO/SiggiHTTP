package io.siggi.http;

import java.io.IOException;

public interface HTTPWebSocketHandler {
	public void handleWebSocket(HTTPWebSocket socket) throws IOException;
}
