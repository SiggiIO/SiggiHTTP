package io.siggi.http.iphelper;

public abstract class IP<IPv extends IP> {

	IP() {
	}

	/**
	 * Create an IP object based on the address or subnet given. This method
	 * will automatically choose whether to create an IPv4 object or an IPv6
	 * object based on the address or subnet.
	 * <p>
	 * If you're passing a subnet, it must be in CIDR Notation.
	 *
	 * @param address
	 * @throws IllegalArgumentException if the IP address is invalid.
	 * @return
	 */
	public static IP getIP(String address) {
		if (address.contains(":")) {
			return new IPv6(address);
		} else {
			return new IPv4(address);
		}
	}

	/**
	 * Check whether this subnet contains the specified address or subnet.
	 * Always returns false if the protocol versions do not match. (for example,
	 * returns false if you're checking an IPv6 address against an IPv4 subnet)
	 *
	 * @param ip the address or subnet to check.
	 * @return whether this subnet contains the specified address or subnet.
	 */
	public final boolean contains(IP ip) {
		if (!this.getClass().isAssignableFrom(ip.getClass())) {
			return false;
		}
		return contains0((IPv) ip);
	}

	abstract boolean contains0(IPv ip);

	/**
	 * Get this address or subnet as a byte array.
	 *
	 * @return the address or subnet as a byte array.
	 */
	public abstract byte[] getBytes();

	protected abstract byte[] getBytes0();

	/**
	 * Get the prefix length of this subnet.
	 *
	 * @return the prefix length of this subnet.
	 */
	public abstract int getPrefixLength();

	/**
	 * Get the number of bytes in this address.
	 *
	 * @return the number of bytes in this address.
	 */
	public abstract int getByteCount();

	/**
	 * Get this address or subnet as a String in long form. The block size is
	 * only included if this object represents a subnet, and not an address.
	 * Square brackets are never included in an IPv6 address, you need to add
	 * square brackets yourself if you're placing the IP in a URL.
	 *
	 * @return the address or subnet as a String in long form.
	 */
	public String toLongString() {
		return toString();
	}

	/**
	 * Get this address or subnet as a String in standard form. The block size
	 * is only included if this object represents a subnet, and not an address.
	 * Square brackets are never included in an IPv6 address, you need to add
	 * square brackets yourself if you're placing the IP in a URL.
	 *
	 * @return the address or subnet as a String in standard form.
	 */
	@Override
	public abstract String toString();

	/**
	 * Get this address or subnet as a String in short form. The block size is
	 * only included if this object represents a subnet, and not an address.
	 * Square brackets are never included in an IPv6 address, you need to add
	 * square brackets yourself if you're placing the IP in a URL.
	 *
	 * @return the address or subnet as a String in short form.
	 */
	public abstract String toShortString();

	protected abstract IPv create(byte[] addr, int block);

	/**
	 * Get the network ID of this IP.
	 *
	 * @return
	 */
	public IPv getNetworkId() {
		byte[] addr = getBytes();
		int blockSize = getPrefixLength();
		return create(trimAddr(addr, blockSize), blockSize);
	}

	/**
	 * Get the subnet mask of this IP.
	 *
	 * @return
	 */
	public IPv getSubnetMask() {
		int byteCount = getByteCount();
		int blockSize = getPrefixLength();
		byte[] mask = new byte[byteCount];
		for (int i = 0; i < mask.length; i++) {
			mask[i] = (byte) filterByte(i, blockSize);
		}
		return create(mask, byteCount * 8);
	}

	/**
	 * Get the wildcard mask of this IP.
	 *
	 * @return
	 */
	public IPv getWildcardMask() {
		int byteCount = getByteCount();
		int blockSize = getPrefixLength();
		byte[] mask = new byte[byteCount];
		for (int i = 0; i < mask.length; i++) {
			mask[i] = (byte) ~filterByte(i, blockSize);
		}
		return create(mask, byteCount * 8);
	}

	/**
	 * Get the first IP for this subnet.
	 *
	 * @return
	 */
	public IPv getFirstIP() {
		int blockSize = getPrefixLength();
		int byteCount = getByteCount();
		if (blockSize == byteCount * 8) {
			return (IPv) this;
		}
		byte[] addr = getBytes();
		for (int i = 0; i < addr.length; i++) {
			addr[i] = (byte) (((int) addr[i]) & filterByte(i, blockSize));
		}
		return create(addr, addr.length * 8);
	}

	/**
	 * Get the last IP for this subnet.
	 *
	 * @return
	 */
	public IPv getLastIP() {
		int blockSize = getPrefixLength();
		int byteCount = getByteCount();
		if (blockSize == byteCount * 8) {
			return (IPv) this;
		}
		byte[] addr = getBytes();
		for (int i = 0; i < addr.length; i++) {
			addr[i] = (byte) (((int) addr[i]) | ~filterByte(i, blockSize));
		}
		return create(addr, addr.length * 8);
	}

	/**
	 * Get the first IP for this subnet.
	 *
	 * @return
	 */
	public IPv getFirstUsableIP() {
		int blockSize = getPrefixLength();
		if (blockSize > 30 || this instanceof IPv6) {
			return getFirstIP();
		}
		int byteCount = getByteCount();
		if (blockSize == byteCount * 8) {
			return (IPv) this;
		}
		byte[] addr = getBytes();
		for (int i = 0; i < addr.length; i++) {
			addr[i] = (byte) (((int) addr[i]) & filterByte(i, blockSize));
		}
		addr[addr.length - 1] += 1;
		return create(addr, addr.length * 8);
	}

	/**
	 * Get the last IP for this subnet.
	 *
	 * @return
	 */
	public IPv getLastUsableIP() {
		int blockSize = getPrefixLength();
		if (blockSize > 30 || this instanceof IPv6) {
			return getLastIP();
		}
		int byteCount = getByteCount();
		if (blockSize == byteCount * 8) {
			return (IPv) this;
		}
		byte[] addr = getBytes();
		for (int i = 0; i < addr.length; i++) {
			addr[i] = (byte) (((int) addr[i]) | ~filterByte(i, blockSize));
		}
		addr[addr.length - 1] -= 1;
		return create(addr, addr.length * 8);
	}

	static byte[] trimAddr(byte[] addr, int block) {
		byte[] newAddr = new byte[addr.length];
		for (int i = 0; i < addr.length; i++) {
			newAddr[i] = (byte) (addr[i] & filterByte(i, block));
		}
		return newAddr;
	}
	private static final int[] masks = new int[]{0x00, 0x80, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC, 0xFE, 0xFF};

	static int filterByte(int index, int blockSize) {
		blockSize -= index * 8;
		blockSize = Math.min(8, Math.max(0, blockSize));
		return masks[blockSize];
	}

	static IllegalArgumentException invalidIP() {
		return new IllegalArgumentException("Invalid IP address!");
	}

	static IllegalArgumentException invalidBlocksize() {
		return new IllegalArgumentException("Invalid blocksize!");
	}

	public boolean getBitAt(int pos) {
		int bytePos = pos / 8;
		int bitPos = pos % 8;
		int shift = (7 - bitPos);
		return ((getBytes0()[bytePos] >> shift) & 0x1) == 1;
	}

	public boolean isSubnetMask() {
		if (this instanceof IPv6){return false;}
		boolean ones = true;
		int c = getByteCount() * 8;
		for (int i = 0; i < c; i++) {
			boolean bit = getBitAt(i);
			if (bit && !ones) {
				return false;
			} else if (!bit && ones) {
				if (i == 0) {
					return false;
				}
				ones = false;
			}
		}
		return true;
	}

	public boolean isWildcardMask() {
		if (this instanceof IPv6){return false;}
		boolean zeroes = true;
		int c = getByteCount() * 8;
		if (!getBitAt(c - 1)) {
			return false;
		}
		for (int i = 0; i < c; i++) {
			boolean bit = getBitAt(i);
			if (!bit && !zeroes) {
				return false;
			} else if (bit && zeroes) {
				zeroes = false;
			}
		}
		return true;
	}

	public int countOneBits() {
		int count = 0;
		int c = getByteCount() * 8;
		for (int i = 0; i < c; i++) {
			if (getBitAt(i)) {
				count += 1;
			}
		}
		return count;
	}

	public int countZeroBits() {
		int count = 0;
		int c = getByteCount() * 8;
		for (int i = 0; i < c; i++) {
			if (!getBitAt(i)) {
				count += 1;
			}
		}
		return count;
	}
}
