package io.siggi.http.iphelper;

import java.util.Arrays;

public final class IPv4 extends IP<IPv4> {

	private final byte[] addr;
	private final int prefixLength;
	private volatile String asString = null;
	private volatile String asShortString = null;

	public IPv4(String ipv4) {
		int blockSize = 32;
		int blockSizeIndex = ipv4.indexOf("/");
		if (blockSizeIndex >= 0) {
			try {
				blockSize = Integer.parseInt(ipv4.substring(blockSizeIndex + 1));
			} catch (NumberFormatException e) {
				throw invalidBlocksize();
			}
			ipv4 = ipv4.substring(0, blockSizeIndex);
		}
		byte[] address = new byte[4];
		String[] parts = ipv4.split("\\.");
		if (parts.length > 4) {
			throw invalidIP();
		}
		if (!(parts.length == 1 && parts[0].equals(""))) {
			for (int i = 0; i < address.length; i++) {
				if (i < parts.length) {
					try {
						address[i] = (byte) Integer.parseInt(parts[i]);
					} catch (NumberFormatException e) {
						throw invalidIP();
					}
				} else {
					address[i] = (byte) 0;
				}
			}
		}
		if (blockSize < 0 || blockSize > 32) {
			throw invalidBlocksize();
		}
		addr = address;
		prefixLength = blockSize;
	}

	public IPv4(byte[] addr) {
		this(addr, 32);
	}

	public IPv4(byte[] addr, int blockSize) {
		if (addr.length != 4) {
			throw invalidIP();
		}
		if (blockSize < 0 || blockSize > 32) {
			throw invalidBlocksize();
		}
		this.addr = new byte[addr.length];
		System.arraycopy(addr, 0, this.addr, 0, addr.length);
		this.prefixLength = blockSize;
	}

	private static String createString(byte[] addr, int block) {
		return Integer.toString(((int) addr[0]) & 0xff)
				+ "." + Integer.toString(((int) addr[1]) & 0xff)
				+ "." + Integer.toString(((int) addr[2]) & 0xff)
				+ "." + Integer.toString(((int) addr[3]) & 0xff)
				+ (block == 32 ? "" : ("/" + Integer.toString(block)));
	}

	private static String createShortString(byte[] addr, int block) {
		StringBuilder shortString = new StringBuilder().append(Integer.toString(((int) addr[0]) & 0xff));
		int x = Math.min((block + 7) / 8, addr.length);
		for (int i = 1; i < x; i++) {
			shortString.append(".").append(Integer.toString(((int) addr[i]) & 0xff));
		}
		if (block != 32) {
			shortString.append("/").append(block);
		}
		return shortString.toString();
	}

	@Override
	boolean contains0(IPv4 ip) {
		for (int i = 0; i < 4; i++) {
			int filter = filterByte(i, prefixLength);
			if ((addr[i] & filter) != (ip.addr[i] & filter)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public byte[] getBytes() {
		byte[] newBytes = new byte[addr.length];
		System.arraycopy(addr, 0, newBytes, 0, addr.length);
		return newBytes;
	}

	@Override
	protected byte[] getBytes0() {
		return addr;
	}

	@Override
	public int getPrefixLength() {
		return prefixLength;
	}

	@Override
	public int getByteCount() {
		return 4;
	}

	@Override
	public String toString() {
		if (asString == null) {
			asString = createString(addr, prefixLength);
		}
		return asString;
	}

	@Override
	public String toShortString() {
		if (asShortString == null) {
			asShortString = createShortString(addr, prefixLength);
		}
		return asShortString;
	}

	@Override
	protected IPv4 create(byte[] addr, int block) {
		return new IPv4(addr, block);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof IPv4) {
			return equals((IPv4) other);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 47 * hash + Arrays.hashCode(this.addr);
		hash = 47 * hash + this.prefixLength;
		return hash;
	}

	public boolean equals(IPv4 other) {
		return Arrays.equals(addr, other.addr) && prefixLength == other.prefixLength;
	}
}
