package io.siggi.http.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamThatClosesOtherResources extends InputStream {
	private final InputStream in;
	private final Closeable[] resources;

	public InputStreamThatClosesOtherResources(InputStream in, Closeable... resources) {
		this.in = in;
		this.resources = resources;
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		return in.read(buffer);
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		return in.read(buffer, offset, length);
	}

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public void close() throws IOException {
		IOException ioe = null;
		try {
			in.close();
		} catch (IOException e) {
			ioe = e;
		}
		for (Closeable closeable : resources) {
			try {
				closeable.close();
			} catch (Exception e) {
			}
		}
		if (ioe != null) throw ioe;
	}

	@Override
	public void mark(int limit) {
		in.mark(limit);
	}

	@Override
	public void reset() throws IOException {
		in.reset();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
}
