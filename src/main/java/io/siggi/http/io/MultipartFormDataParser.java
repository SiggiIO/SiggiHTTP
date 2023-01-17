package io.siggi.http.io;

import io.siggi.http.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class MultipartFormDataParser {
	private final EOFInputStream in;
	private final String boundary;
	private final int headerSizeLimit;
	private final EOFInputStream partReader;
	private boolean firstRead = true;

	public MultipartFormDataParser(InputStream in, String boundary, int headerSizeLimit) throws IOException {
		if (in instanceof EOFInputStream) {
			this.in = (EOFInputStream) in;
		} else {
			this.in = new EOFInputStream(in);
		}
		if (boundary.contains("boundary=")) {
			String newBoundary = Util.parseHeaderArguments(boundary).get("boundary");
			if (newBoundary != null) boundary = newBoundary;
		}
		this.boundary = boundary;
		this.headerSizeLimit = headerSizeLimit;
		partReader = new EOFInputStream(this.in);

		initialize();
	}

	private void initialize() throws IOException {
		in.setEofSequence(("\r\n--" + boundary + "--\r\n").getBytes());
		partReader.setEofSequence(("--" + boundary + "\r\n").getBytes());
		Util.readFullyToBlackHole(partReader);
		partReader.setEofSequence(("\r\n--" + boundary + "\r\n").getBytes());
		partReader.nextEofSequence();
	}

	public Part nextPart() throws IOException {
		if (firstRead) {
			firstRead = false;
		} else {
			Util.readFullyToBlackHole(partReader);
			partReader.nextEofSequence();
		}
		Map<String, List<String>> partHeaders = Util.readHeaders(partReader, headerSizeLimit);
		if (partHeaders == null) {
			return null;
		}
		String name = null;
		String filename = null;
		long contentLength = -1L;
		String contentType = Util.getFirstInList(partHeaders.get("Content-Type"));
		String contentDisposition = Util.getFirstInList(partHeaders.get("Content-Disposition"));
		if (contentDisposition != null) {
			Map<String, String> map = Util.parseHeaderArguments(contentDisposition);
			name = map.get("name");
			filename = map.get("filename");
		}
		String contentLengthString = Util.getFirstInList(partHeaders.get("Content-Length"));
		if (contentLengthString != null) {
			try {
				contentLength = Long.parseLong(contentLengthString);
			} catch (Exception e) {
			}
		}
		return new Part(partReader, partHeaders, name, filename, contentType, contentLength);
	}

	public class Part {
		private Part(InputStream inputStream, Map<String,List<String>> headers, String name, String filename, String contentType, long contentLength) {
			this.inputStream = inputStream;
			this.headers = headers;
			this.name = name;
			this.filename = filename;
			this.contentType = contentType;
			this.contentLength = contentLength;
		}

		private final InputStream inputStream;
		private final Map<String,List<String>> headers;
		private final String name;
		private final String filename;
		private final String contentType;
		private final long contentLength;
		private String value;

		/**
		 * Get the InputStream to read data from this form part.
		 *
		 * @return the InputStream to read data from
		 */
		public InputStream getInputStream() {
			return inputStream;
		}

		/**
		 * Get the headers for this form part.
		 *
		 * @return the headers for this form part.
		 */
		public Map<String,List<String>> getHeaders() {
			return headers;
		}

		/**
		 * Get the list of headers by this key for this form part.
		 *
		 * @param key the header key
		 * @return a list of header values
		 */
		public List<String> getHeaders(String key) {
			return headers.get(key);
		}

		/**
		 * Get the top header by this key for this form part.
		 *
		 * @param key the header key
		 * @return the header value
		 */
		public String getHeader(String key) {
			List<String> strings = headers.get(key);
			if (strings == null || strings.isEmpty()) return null;
			return strings.get(0);
		}

		/**
		 * Get the form field name for this form part.
		 *
		 * @return The form field name.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Get whether this form field contains a file.
		 *
		 * @return true if the field contains a file
		 */
		public boolean isFile() {
			return filename != null;
		}

		/**
		 * Get the file name for this form part, null if it is not a file field.
		 *
		 * @return the file name
		 */
		public String getFilename() {
			return filename;
		}

		/**
		 * Get the content type for this file.
		 *
		 * @return the content type of the file
		 */
		public String getContentType() {
			return contentType;
		}

		/**
		 * Get the content length for this form field which may be -1 indicating it is unknown.
		 *
		 * @return the content length of the form field
		 */
		public long getContentLength() {
			return contentLength;
		}

		/**
		 * Get the value of the form field by reading from the InputStream. Use only InputStream or only getValue() but not both.
		 *
		 * @return the value of the form field.
		 * @throws IOException if something goes wrong
		 */
		public String getValue() throws IOException {
			return getValue(2097152);
		}

		/**
		 * Get the value of the form field by reading from the InputStream with a custom maximum length.
		 *
		 * @return the value of the form field.
		 * @throws IOException if something goes wrong
		 */
		public String getValue(int maxLength) throws IOException {
			if (value == null) {
				byte[] bytes = Util.readFully(inputStream, maxLength);
				value = new String(bytes, StandardCharsets.UTF_8);
			}
			return value;
		}
	}
}
