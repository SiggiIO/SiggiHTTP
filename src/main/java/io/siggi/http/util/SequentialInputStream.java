package io.siggi.http.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class SequentialInputStream extends InputStream {

	private final LinkedList<InputStream> streams = new LinkedList<>();
	private InputStream currentStream = null;

	public SequentialInputStream(List<InputStream> streams) {
		this.streams.addAll(streams);
		nextStream();
	}

	@Override
	public int read() throws IOException {
		if (currentStream == null) {
			return -1;
		}
		do {
			int r = currentStream.read();
			if (r != -1) {
				return r;
			}
		} while (nextStream());
		return -1;
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		if (currentStream == null) {
			return -1;
		}
		do {
			int r = currentStream.read(buffer);
			if (r == -1) {
				return r;
			}
		} while (nextStream());
		return -1;
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		if (currentStream == null) {
			return -1;
		}
		do {
			int r = currentStream.read(buffer, off, len);
			if (r == -1) {
				return r;
			}
		} while (nextStream());
		return -1;
	}

	@Override
	public void close() {
		while (currentStream != null) {
			try {
				currentStream.close();
			} catch (Exception e) {
			}
			nextStream();
		}
	}

	private boolean nextStream() {
		if (currentStream != null) {
			try {
				currentStream.close();
			} catch (Exception e) {
			}
		}
		try {
			currentStream = streams.pop();
			return true;
		} catch (NoSuchElementException nsee) {
			currentStream = null;
			return false;
		}
	}
}
