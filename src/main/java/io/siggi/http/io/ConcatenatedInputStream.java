package io.siggi.http.io;

import java.io.IOException;
import java.io.InputStream;

public final class ConcatenatedInputStream extends InputStream {

	public ConcatenatedInputStream(InputStream firstStream, InputStream secondStream) {
		this.firstStream = firstStream;
		this.stream = secondStream;
	}
	private InputStream firstStream;
	private final InputStream stream;

	private void closeFirstStream() {
		try {
			firstStream.close();
		} catch (Exception e) {
		}
		firstStream = null;
	}

	@Override
	public int read() throws IOException {
		if (firstStream != null) {
			int v = firstStream.read();
			if (v < 0) {
				closeFirstStream();
			} else {
				return v;
			}
		}
		return stream.read();
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		if (firstStream != null) {
			int v = firstStream.read(buffer);
			if (v < 0) {
				closeFirstStream();
			} else {
				return v;
			}
		}
		return stream.read(buffer);
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (firstStream != null) {
			int v = firstStream.read(buffer, offset, length);
			if (v < 0) {
				closeFirstStream();
			} else {
				return v;
			}
		}
		return stream.read(buffer, offset, length);
	}

	@Override
	public long skip(long n) throws IOException {
		if (firstStream != null) {
			long v = firstStream.skip(n);
			if (v <= 0) {
				closeFirstStream();
			} else {
				return v;
			}
		}
		return stream.skip(n);
	}

	@Override
	public int available() throws IOException {
		return firstStream == null ? stream.available() : firstStream.available() + stream.available();
	}

	@Override
	public void close() throws IOException {
		closeFirstStream();
		stream.close();
	}
}
