package io.siggi.http;

import java.io.UnsupportedEncodingException;

public class HTTPWebSocketMessage {

	public static final int OPCODE_CONTINUATION = 0;
	public static final int OPCODE_TEXT = 1;
	public static final int OPCODE_BINARY = 2;
	public static final int OPCODE_CLOSE = 8;
	public static final int OPCODE_PING = 9;
	public static final int OPCODE_PONG = 10;

	private static final byte[] zeroBytes = new byte[0];

	HTTPWebSocketMessage(int opcode, byte[] bytes) {
		this.opcode = opcode;
		this.bytes = bytes;
	}

	/**
	 * Create a text message
	 *
	 * @param message the text message
	 * @return the message
	 */
	public static HTTPWebSocketMessage create(String message) {
		try {
			return new HTTPWebSocketMessage(1, message.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Create a binary message
	 *
	 * @param message the bytes
	 * @return the message
	 */
	public static HTTPWebSocketMessage create(byte[] message) {
		byte[] msg = new byte[message.length];
		System.arraycopy(message, 0, msg, 0, message.length);
		return new HTTPWebSocketMessage(2, msg);
	}

	/**
	 * Create a control message
	 *
	 * @param opcode the opcode
	 * @return the message
	 * @throws IllegalArgumentException if the passed opcode is not a control
	 * opcode
	 */
	public static HTTPWebSocketMessage create(int opcode) {
		if (opcode < 0x08) {
			throw new IllegalArgumentException("This is not a control opcode!");
		}
		return new HTTPWebSocketMessage(opcode, zeroBytes);
	}

	private final int opcode;
	private final byte[] bytes;

	public boolean isText() {
		return opcode == OPCODE_TEXT;
	}

	public boolean isBinary() {
		return opcode == OPCODE_BINARY;
	}

	public boolean isControlMessage() {
		return opcode >= 0x8;
	}

	public int getOpcode() {
		return opcode;
	}

	public int getLength() {
		return bytes.length;
	}

	public byte[] getBytes() {
		byte[] b = new byte[bytes.length];
		System.arraycopy(bytes, 0, b, 0, bytes.length);
		return b;
	}

	public void getBytes(byte[] b) {
		System.arraycopy(bytes, 0, b, 0, Math.min(bytes.length, b.length));
	}

	public String getText() {
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}
}
