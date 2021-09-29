package io.siggi.http.simpleresponders;

import io.siggi.http.HTTPRequest;
import io.siggi.http.HTTPResponder;
import io.siggi.http.io.InputStreamThatClosesOtherResources;
import io.siggi.http.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileResponder implements HTTPResponder {

	private final String mountPath;
	private final File zipFile;
	private final ReentrantReadWriteLock propsLock = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock.ReadLock propsReadLock = propsLock.readLock();
	private final ReentrantReadWriteLock.WriteLock propsWriteLock = propsLock.writeLock();
	private long lastModified = 0L;
	private Properties props = null;

	public ZipFileResponder(String mountPath, File zipFile) {
		while (mountPath.endsWith("//")) {
			mountPath = mountPath.substring(0, mountPath.length() - 1);
		}
		if (!mountPath.endsWith("/")) {
			mountPath += "/";
		}
		this.mountPath = mountPath;
		this.zipFile = zipFile;
	}

	@Override
	public void respond(HTTPRequest request) throws Exception {
		if (!request.url.startsWith(mountPath)) {
			return;
		}
		String requestedPath = request.url.substring(mountPath.length());
		if (requestedPath.equals("info.txt")) {
			return;
		}
		String acceptedEncodingsString = request.getHeader("Accept-Encoding");
		String[] acceptedEncodings = acceptedEncodingsString.replace(" ", "").split(",");
		boolean allowGzip = false;
		boolean allowBrotli = false;
		for (String acceptedEncoding : acceptedEncodings) {
			if (acceptedEncoding.contains(";")) {
				acceptedEncoding = acceptedEncoding.substring(0, acceptedEncoding.indexOf(";")).trim();
			}
			switch (acceptedEncoding.toLowerCase()) {
				case "br":
					allowBrotli = true;
					break;
				case "gzip":
					allowGzip = true;
					break;
			}
		}
		try (ZipFile zf = new ZipFile(zipFile)) {
			Properties properties = getProperties();
			long maxAge = 86400L;
			try {
				maxAge = Long.parseLong(properties.getProperty("max-age"));
			} catch (Exception e) {
			}
			String rename = properties.getProperty("rename:" + requestedPath);
			if (rename != null) {
				request.response.redirect(mountPath + rename);
				return;
			}
			String sha1 = properties.getProperty("sha1:" + requestedPath);
			ZipEntry brotliEntry = zf.getEntry(requestedPath + ".br");
			ZipEntry gzipEntry = zf.getEntry(requestedPath + ".gz");
			ZipEntry uncompressedEntry = zf.getEntry(requestedPath);
			if (allowBrotli && brotliEntry != null && !brotliEntry.isDirectory()) {
				respond(request, zf, brotliEntry, "br", maxAge, sha1);
			} else if (allowGzip && gzipEntry != null && !gzipEntry.isDirectory()) {
				respond(request, zf, gzipEntry, "gzip", maxAge, sha1);
			} else if (uncompressedEntry != null && !uncompressedEntry.isDirectory()) {
				respond(request, zf, uncompressedEntry, null, maxAge, sha1);
			}
		}
	}

	private void respond(HTTPRequest request, ZipFile zf, ZipEntry entry, String encoding, long maxAge, String fileSha) throws Exception {
		if (fileSha != null) {
			String eTag = "\"" + fileSha + (encoding == null ? "" : ("-" + encoding)) + "\"";
			String ifNoneMatch = request.getHeader("If-None-Match");
			if (ifNoneMatch.equals(eTag)) {
				request.response.setHeader("304 Not Modified");
				request.response.sendHeaders();
			} else {
				request.response.setHeader("ETag", eTag);
			}
		}
		if (encoding != null) {
			request.response.setHeader("Content-Encoding", encoding);
		}
		request.response.contentLength(entry.getSize());
		if (maxAge > 0L) {
			request.response.cache(maxAge);
			request.response.setHeader("Vary", "Accept-Encoding");
		} else {
			request.response.doNotCache();
		}
		Util.copy(zf.getInputStream(entry), request.response);
	}

	public InputStream getFileStream(String file) throws IOException {
		ZipFile zf = null;
		try {
			zf = new ZipFile(zipFile);
			Properties properties = getProperties();
			String rename = properties.getProperty("rename:" + file);
			if (rename != null) {
				file = rename;
			}
			ZipEntry entry = zf.getEntry(file);
			if (entry == null) {
				throw new FileNotFoundException(zipFile.getPath() + ":" + file);
			}
			return new InputStreamThatClosesOtherResources(zf.getInputStream(entry), zf);
		} catch (IOException ioe) {
			if (zf != null) {
				try {
					zf.close();
				} catch (IOException ioe2) {
				}
			}
			throw ioe;
		}
	}

	public Properties getProperties() {
		propsReadLock.lock();
		try {
			long lm = zipFile.lastModified();
			if (props != null && lastModified == lm) {
				return props;
			}
		} finally {
			propsReadLock.unlock();
		}
		propsWriteLock.lock();
		try {
			long lm = zipFile.lastModified();
			if (props != null && lastModified == lm) {
				return props;
			}
			try (ZipFile zf = new ZipFile(zipFile)) {
				props = getProperties(zf);
				lastModified = lm;
			} catch (IOException e) {
			}
			return props;
		} finally {
			propsWriteLock.unlock();
		}
	}

	private Properties getProperties(ZipFile zf) throws IOException {
		Properties properties = new Properties();
		properties.load(zf.getInputStream(zf.getEntry("info.txt")));
		return properties;
	}
}
