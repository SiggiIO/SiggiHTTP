package io.siggi.http;

import io.siggi.http.session.Session;
import io.siggi.http.session.Sessions;
import io.siggi.http.util.CaseInsensitiveHashMap;
import io.siggi.http.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HTTPWebSocket implements Closeable {

	private final HTTPHandler handler;
	private final Socket socket;
	private final InputStream in;
	private final OutputStream out;
	private final String wsNonce;
	private final String resultNonce;
	public final String method;
	public final String requestURI;
	public final String fullRequestURI;
	public final Map<String, String> get;
	public final Map<String, String> cookies;
	public final Map<String, List<String>> headers;
	public final String host;
	public final String referer;
	public final String userAgent;

	private final Map<String, List<String>> outputHeaders;
	private boolean sentCloseFrame = false;
	private boolean nonBlockingMode = false;
	private final Object sendQueueLock = new Object();
	private boolean closed = false;
	private final List<HTTPWebSocketMessage> sendQueue = new LinkedList<>();

	private boolean accepted = false;

	private final int id;
	private static int nextId = 0;

	private static synchronized int id() {
		return nextId++;
	}

	HTTPWebSocket(HTTPHandler handler, Socket socket, InputStream in, OutputStream out, String wsNonce, String resultNonce, String method, String requestURI, String fullRequestURI, Map<String, String> get, Map<String, String> cookies, Map<String, List<String>> headers, String host, String referer, String userAgent) {
		this.handler = handler;
		this.socket = socket;
		this.in = in;
		this.out = out;
		this.wsNonce = wsNonce;
		this.resultNonce = resultNonce;
		this.method = method;
		this.requestURI = requestURI;
		this.fullRequestURI = fullRequestURI;
		this.get = get;
		this.cookies = cookies;
		this.headers = headers;
		this.host = host;
		this.referer = referer;
		this.userAgent = userAgent;
		this.outputHeaders = new CaseInsensitiveHashMap<>();
		this.id = id();
		setHeader("Upgrade", "websocket");
		setHeader("Connection", "Upgrade");
		setHeader("Sec-WebSocket-Accept", resultNonce);
		String protocol = null;
		List<String> p = headers.get("Sec-WebSocket-Protocol");
		if (p != null && !p.isEmpty()) {
			String proto = p.get(0);
			int x = proto.indexOf(",");
			if (x >= 0) {
				proto = proto.substring(0, x);
			}
			proto = proto.trim();
			protocol = proto;
		}
		if (protocol != null) {
			setHeader("Sec-WebSocket-Protocol", protocol);
		}
	}

	Session session;

	/**
	 * Get the current session, which may be null if there is no session prior
	 * to starting the websocket.
	 *
	 * @return the session
	 */
	public Session getSession() {
		if (session == null || session.isDeleted()) {
			String sessionCookieName = handler.server.getSessionCookieName();
			Sessions sessions = handler.server.getSessions();
			String sessionId = cookies.get(sessionCookieName);
			if (sessionId == null) {
				session = null;
			} else {
				session = sessions.get(sessionId);
			}
		}
		return session;
	}

	@SuppressWarnings("deprecation")
	void saveSession() {
		if (session != null) {
			session.save();
		}
	}

	private void writeHeaders() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Util.writeCRLF("HTTP/1.1 101 Switching Protocols", baos);
		for (Map.Entry<String, List<String>> entry : outputHeaders.entrySet()) {
			String header = entry.getKey();
			List<String> list = entry.getValue();
			for (String str : list) {
				Util.writeCRLF(header + ": " + str, baos);
			}
		}
		Util.writeCRLF("", baos);
		out.write(baos.toByteArray());
	}

	public void setHeader(String key, String val) {
		if (accepted) {
			throw new IllegalStateException("Socket already accepted, cannot change headers!");
		}
		List<String> v = new ArrayList<>();
		v.add(val);
		outputHeaders.put(key, v);
	}

	public void addHeader(String key, String val) {
		if (accepted) {
			throw new IllegalStateException("Socket already accepted, cannot change headers!");
		}
		List<String> v = outputHeaders.get(key);
		if (v == null) {
			setHeader(key, val);
		} else {
			v.add(val);
		}
	}

	public void accept() throws IOException {
		if (accepted) {
			return;
		}
		socket.setSoTimeout(60000);
		accepted = true;
		try {
			writeHeaders();
		} catch (IOException e) {
			try {
				socket.close();
			} catch (Exception e2) {
			}
			throw e;
		}
	}

	public void useNonBlockingMode() {
		if (!accepted) {
			throw new IllegalStateException("Socket hasn't been accepted!");
		}
		synchronized (sendQueueLock) {
			if (nonBlockingMode) {
				return;
			}
			nonBlockingMode = true;
		}
		new Thread(this::incomingThread, "WebSocket-" + id + "-In").start();
		new Thread(this::outgoingThread, "WebSocket-" + id + "-Out").start();
	}

	public HTTPWebSocketMessage read() throws IOException {
		if (nonBlockingMode) {
			throw new IllegalStateException("Non blocking mode in use! Add an event listener instead!");
		}
		if (!accepted) {
			throw new IllegalStateException("Socket hasn't been accepted!");
		}
		return read0();
	}

	private HTTPWebSocketMessage read0() throws IOException {
		int opcode = 0;
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		while (true) {
			WebSocketPacket pkt = read1();
			if (pkt == null) {
				return null;
			}
			if (pkt.opcode > 0) {
				o.reset();
				opcode = pkt.opcode;
			}
			o.write(pkt.decoded);
			if (pkt.fin) {
				HTTPWebSocketMessage msg = new HTTPWebSocketMessage(opcode, o.toByteArray());
				o.reset();
				return msg;
			}
		}
	}

	private WebSocketPacket read1() throws IOException {
		int opcode = in.read();
		if (opcode == -1) {
			return null;
		}
		boolean fin = (opcode & 0x80) != 0;
		boolean rsv1 = (opcode & 0x40) != 0;
		boolean rsv2 = (opcode & 0x20) != 0;
		boolean rsv3 = (opcode & 0x10) != 0;
		opcode = opcode & 0x0F;
		long lengthOfPayload = (long) in.read();
		if (opcode == -1L) {
			return null;
		}
		boolean mask = (lengthOfPayload & 0x80L) != 0L;
		lengthOfPayload = lengthOfPayload & 0x7F;
		if (lengthOfPayload == 126L) {
			lengthOfPayload = (long) ((in.read() << 8) + in.read());
		} else if (lengthOfPayload == 127L) {
			lengthOfPayload = ((long) in.read() << 56)
					+ ((long) in.read() << 48)
					+ ((long) in.read() << 40)
					+ ((long) in.read() << 32)
					+ ((long) in.read() << 24)
					+ ((long) in.read() << 16)
					+ ((long) in.read() << 8)
					+ (long) in.read();
		}
		if (lengthOfPayload > handler.server.getWebsocketMaxPayloadLength()) {
			return null;
		}
		byte[] maskKey = new byte[4];
		int amountRead = 0;
		int c;
		if (mask) {
			while (amountRead < 4) {
				c = in.read(maskKey, amountRead, 4 - amountRead);
				if (c == -1) {
					return null;
				}
				amountRead += c;
			}
		}
		byte[] message = new byte[(int) lengthOfPayload];
		amountRead = 0;
		while (amountRead < lengthOfPayload) {
			c = in.read(message, amountRead, message.length - amountRead);
			if (c == -1) {
				return null;
			}
			amountRead += c;
		}
		byte[] decoded = new byte[message.length];
		if (mask) {
			for (int i = 0; i < message.length; i++) {
				decoded[i] = (byte) ((((int) message[i]) & 0xff) ^ (((int) maskKey[i % 4]) & 0xff));
			}
		} else {
			System.arraycopy(message, 0, decoded, 0, message.length);
		}
		return new WebSocketPacket(fin, rsv1, rsv2, rsv3, opcode, mask, maskKey, lengthOfPayload, message, decoded);
	}

	public void send(HTTPWebSocketMessage msg) throws IOException {
		if (!accepted) {
			throw new IllegalStateException("Socket hasn't been accepted!");
		}
		synchronized (sendQueueLock) {
			if (msg.getOpcode() == HTTPWebSocketMessage.OPCODE_CLOSE) {
				if (sentCloseFrame) {
					return;
				}
				sentCloseFrame = true;
			} else {
				if (sentCloseFrame) {
					throw new IllegalStateException("Already sent close frame!");
				}
			}
			if (nonBlockingMode) {
				sendQueue.add(msg);
				sendQueueLock.notifyAll();
			} else {
				send0(msg);
			}
		}
	}

	private void send0(HTTPWebSocketMessage msg) throws IOException {
		int headerLength = 2;
		byte[] header = new byte[10];
		header[0] = (byte) (0x80 + (msg.getOpcode() & 0xF));
		int len = msg.getLength();
		if (len >= 126 && len <= 65535) {
			headerLength = 4;
			header[1] = (byte) (126);
			header[2] = (byte) ((len >> 8) & 0xff);
			header[3] = (byte) (len & 0xff);
		} else if (len >= 65536) {
			headerLength = 10;
			header[1] = (byte) 127;
			header[2] = (byte) 0;
			header[3] = (byte) 0;
			header[4] = (byte) 0;
			header[5] = (byte) 0;
			header[6] = (byte) ((len >> 24) & 0xff);
			header[7] = (byte) ((len >> 16) & 0xff);
			header[8] = (byte) ((len >> 8) & 0xff);
			header[9] = (byte) (len & 0xff);
		} else {
			header[1] = (byte) (len);
		}
		out.write(header, 0, headerLength);
		out.write(msg.getBytes());
		out.flush();
	}

	@Override
	public void close() throws IOException {
		if (!accepted) {
			try {
			} catch (Exception e) {
				socket.close();
			}
			return;
		}
		synchronized (sendQueueLock) {
			if (closed) {
				return;
			}
			closed = true;
			saveSession();
			sendQueueLock.notifyAll();
		}
		send(HTTPWebSocketMessage.create(HTTPWebSocketMessage.OPCODE_CLOSE));
		socket.close();
	}

	private final Set<HTTPWebSocketListener> listeners = new HashSet<>();

	public void addListener(HTTPWebSocketListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeListener(HTTPWebSocketListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private void incomingThread() {
		try {
			while (true) {
				HTTPWebSocketMessage msg = read0();
				if (msg == null) {
					break;
				}
				synchronized (listeners) {
					for (HTTPWebSocketListener listener : listeners) {
						try {
							listener.receivedMessage(this, msg);
						} catch (Exception e) {
						}
					}
				}
			}
		} catch (IOException e) {
		}
		try {
			close();
		} catch (Exception e) {
		}
		synchronized (listeners) {
			for (HTTPWebSocketListener listener : listeners) {
				listener.socketClosed(this);
			}
		}
	}

	private void outgoingThread() {
		long lastSentPing = System.currentTimeMillis();
		try {
			a:
			while (true) {
				HTTPWebSocketMessage msg;
				synchronized (sendQueueLock) {
					if (closed) {
						break;
					}
					while (sendQueue.isEmpty()) {
						if (closed) {
							break a;
						}
						long now = System.currentTimeMillis();
						boolean needToSendPing = (now - lastSentPing) >= 10000L;
						long nextPing;
						if (needToSendPing) {
							lastSentPing = now;
							send0(HTTPWebSocketMessage.create(HTTPWebSocketMessage.OPCODE_PING));
							nextPing = 10000L;
						} else {
							nextPing = 10000L - (now - lastSentPing);
						}
						try {
							sendQueueLock.wait(nextPing);
						} catch (InterruptedException e) {
							break a;
						}
						now = System.currentTimeMillis();
						needToSendPing = (now - lastSentPing) >= 10000L;
						if (needToSendPing) {
							lastSentPing = now;
							send0(HTTPWebSocketMessage.create(HTTPWebSocketMessage.OPCODE_PING));
						}
					}
					msg = sendQueue.remove(0);
				}
				if (msg != null) {
					send0(msg);
				}
			}
		} catch (IOException e) {
		}
		try {
			close();
		} catch (Exception e) {
		}
	}

	private static class WebSocketPacket {

		private final boolean fin;
		private final boolean rsv1;
		private final boolean rsv2;
		private final boolean rsv3;
		private final int opcode;
		private final boolean mask;
		private final byte[] maskKey;
		private final long lengthOfPayload;
		private final byte[] message;
		private final byte[] decoded;

		private WebSocketPacket(boolean fin, boolean rsv1, boolean rsv2, boolean rsv3, int opcode, boolean mask, byte[] maskKey, long lengthOfPayload, byte[] message, byte[] decoded) {
			this.fin = fin;
			this.rsv1 = rsv1;
			this.rsv2 = rsv2;
			this.rsv3 = rsv3;
			this.opcode = opcode;
			this.mask = mask;
			this.maskKey = maskKey;
			this.lengthOfPayload = lengthOfPayload;
			this.message = message;
			this.decoded = decoded;
		}

	}
}
