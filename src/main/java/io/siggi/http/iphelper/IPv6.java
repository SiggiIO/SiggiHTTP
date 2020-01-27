package io.siggi.http.iphelper;

import java.util.Arrays;

public final class IPv6 extends IP<IPv6> {

	private final byte[] addr;
	private final int prefixLength;
	private volatile String asLongString = null;
	private volatile String asString = null;
	private volatile String asShortString = null;

	public IPv6(String ipv6) {
		ipv6 = ipv6.replace("[", "").replace("]", "");
		int blockSize = 128;
		int blockSizeIndex = ipv6.indexOf("/");
		if (blockSizeIndex >= 0) {
			try {
				blockSize = Integer.parseInt(ipv6.substring(blockSizeIndex + 1));
			} catch (NumberFormatException e) {
				throw invalidBlocksize();
			}
			ipv6 = ipv6.substring(0, blockSizeIndex);
		}
		int stringLength = ipv6.length();
		int doubleColonPosition = ipv6.indexOf("::");
		{
			if (!ipv6.contains(":")) {
				throw invalidIP();
			}
			if (doubleColonPosition >= 0) {
				if (ipv6.indexOf("::", doubleColonPosition + 1) != -1) {
					throw invalidIP();
				}
			}
		}
		byte[] address = new byte[16];

		parse:
		if (doubleColonPosition == -1) { // no double colon
			String[] parts = ipv6.split(":");
			if (parts.length != 8) {
				throw invalidIP();
			}
			for (int i = 0; i < 8; i++) {
				int n = Integer.parseInt(parts[i], 16);
				if (n > 0xFFFF || n < 0) {
					throw invalidIP();
				}
				int n1 = (n >> 8) & 0xff;
				int n2 = n & 0xff;
				address[i * 2] = (byte) n1;
				address[(i * 2) + 1] = (byte) n2;
			}
		} else if (doubleColonPosition == 0) { // double colon at beginning
			if (ipv6.length() == 2) { // double colon only
				// nothing to do, we already have the address in the bytearray.
				break parse;
			}
			String[] parts = ipv6.substring(2).split(":");
			int add = 8 - parts.length;
			for (int i = 0; i < parts.length; i++) {
				int n = Integer.parseInt(parts[i], 16);
				if (n > 0xFFFF || n < 0) {
					throw invalidIP();
				}
				int n1 = (n >> 8) & 0xff;
				int n2 = n & 0xff;
				address[(i + add) * 2] = (byte) n1;
				address[((i + add) * 2) + 1] = (byte) n2;
			}
		} else if (doubleColonPosition == stringLength - 2) { // double colon at end
			String[] parts = ipv6.substring(0, ipv6.length() - 2).split(":");
			for (int i = 0; i < parts.length; i++) {
				int n = Integer.parseInt(parts[i], 16);
				if (n > 0xFFFF || n < 0) {
					throw invalidIP();
				}
				int n1 = (n >> 8) & 0xff;
				int n2 = n & 0xff;
				address[i * 2] = (byte) n1;
				address[(i * 2) + 1] = (byte) n2;
			}
		} else { // double colon in middle
			String[] parts = ipv6.split(":");
			int pos = -1;
			int skipCount = 8 - parts.length;
			for (int i = 0; i < parts.length; i++) {
				pos += 1;
				if (parts[i].equals("")) {
					pos += skipCount;
					continue;
				}
				int n = Integer.parseInt(parts[i], 16);
				if (n > 0xFFFF || n < 0) {
					throw invalidIP();
				}
				int n1 = (n >> 8) & 0xff;
				int n2 = n & 0xff;
				address[pos * 2] = (byte) n1;
				address[(pos * 2) + 1] = (byte) n2;
			}
		}
		if (blockSize < 0 || blockSize > 128) {
			throw invalidBlocksize();
		}
		this.addr = address;
		this.prefixLength = blockSize;
	}

	public IPv6(byte[] addr) {
		this(addr, 128);
	}

	public IPv6(byte[] addr, int blockSize) {
		if (addr.length != 16) {
			throw invalidIP();
		}
		if (blockSize < 0 || blockSize > 128) {
			throw invalidBlocksize();
		}
		this.addr = new byte[addr.length];
		System.arraycopy(addr, 0, this.addr, 0, addr.length);
		this.prefixLength = blockSize;
	}

	private static String createString(byte[] addr, boolean longForm) {
		if (addr.length != 16) {
			throw invalidIP();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			if (i != 0) {
				sb.append(":");
			}
			int n1 = addr[i * 2] & 0xff;
			int n2 = addr[(i * 2) + 1] & 0xff;
			int n = (n1 << 8) + n2;
			String str = Integer.toString(n, 16);
			if (longForm) {
				for (int j = 4; j > str.length(); j--) {
					sb.append("0");
				}
			}
			sb.append(str);
		}
		return sb.toString();
	}

	private static String createLongString(byte[] addr, int block) {
		return createString(addr, true) + (block == 128 ? "" : ("/" + block));
	}

	private static String createString(byte[] addr, int block) {
		return createString(addr, false) + (block == 128 ? "" : ("/" + block));
	}

	private static String createShortString(byte[] addr, int block) {
		String addressStr = createString(addr, false);
		String[] parts = addressStr.split(":");
		int shortPoint = -1;
		int shortLength = -1;
		int checkPoint = -1;
		int checkLength = -1;
		for (int i = 0; i < parts.length; i++) {
			if (Integer.parseInt(parts[i], 16) == 0) {
				if (checkPoint == -1) {
					checkPoint = i;
					checkLength = 1;
				} else {
					checkLength += 1;
				}
			} else if (checkPoint != -1) {
				if (checkLength > shortLength) {
					shortPoint = checkPoint;
					shortLength = checkLength;
				}
				checkPoint = -1;
				checkLength = -1;
			}
		}
		if (checkPoint != -1) {
			if (checkLength > shortLength) {
				shortPoint = checkPoint;
				shortLength = checkLength;
			}
		}
		if (shortPoint != -1) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < parts.length; i++) {
				if (i >= shortPoint && i < shortPoint + shortLength) {
					if (i == shortPoint) {
						sb.append(":");
					}
				} else {
					if (i != 0) {
						sb.append(":");
					}
					sb.append(parts[i]);
				}
			}
			if (sb.charAt(sb.length() - 1) == ':') {
				if (sb.length() == 1 || sb.charAt(sb.length() - 2) != ':') {
					sb.append(':');
				}
			}
			addressStr = sb.toString();
		}
		return addressStr + (block == 128 ? "" : ("/" + block));
	}

	@Override
	boolean contains0(IPv6 ip) {
		for (int i = 0; i < 16; i++) {
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
		return 16;
	}

	@Override
	public String toLongString() {
		if (asLongString == null) {
			asLongString = createLongString(addr, prefixLength);
		}
		return asLongString;
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
	protected IPv6 create(byte[] addr, int block) {
		return new IPv6(addr, block);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof IPv6) {
			return equals((IPv6) other);
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

	public boolean equals(IPv6 other) {
		return Arrays.equals(addr, other.addr) && prefixLength == other.prefixLength;
	}

	public boolean isMacAddressBased() {
		return addr[11] == (byte) 0xFF && addr[12] == (byte) 0xFE;
	}
}
