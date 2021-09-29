package io.siggi.http.simpleresponders;

import io.siggi.http.HTTPRequest;
import io.siggi.http.HTTPResponder;
import io.siggi.http.util.Util;
import io.siggi.ziplib.ZipArchive;
import io.siggi.ziplib.ZipArchiveEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static io.siggi.http.util.Util.getFileExtension;

public class ZipFileResponder implements HTTPResponder {

	private static final byte[] gzipHeader = new byte[]{(byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x03};

	private final String mountPath;
	private final File zipFile;
	private final ReentrantReadWriteLock archiveLock = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock.ReadLock archiveReadLock = archiveLock.readLock();
	private final ReentrantReadWriteLock.WriteLock archiveWriteLock = archiveLock.writeLock();
	private long lastModified = 0L;
	private ZipArchive archive = null;
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
		ZipArchive zipArchive = ZipArchive.from(zipFile);
		Properties properties = getProperties();
		long maxAge = 86400L;
		try {
			maxAge = Long.parseLong(getOption(requestedPath, properties, "max-age"));
		} catch (Exception e) {
		}
		boolean autoRedirect = true;
		try {
			String autoRedirectString = getOption(requestedPath, properties, "auto-redirect");
			if (autoRedirectString != null && (autoRedirectString.equals("0") || autoRedirectString.equals("no") || autoRedirectString.equals("false"))) {
				autoRedirect = false;
			}
		} catch (Exception e) {
		}
		String rename = properties.getProperty("rename:" + requestedPath);
		if (rename != null) {
			if (autoRedirect) {
				request.response.redirect(mountPath + rename);
			}
			return;
		}
		String extension = getFileExtension(requestedPath);
		String contentType = properties.getProperty("mime:" + extension);
		if (contentType == null) {
			contentType = request.getMimeType(extension);
		}
		String sha1 = properties.getProperty("sha1:" + requestedPath);
		ZipArchiveEntry brotliEntry = zipArchive.getEntry(requestedPath + ".br");
		ZipArchiveEntry uncompressedEntry = zipArchive.getEntry(requestedPath);
		if (allowBrotli && brotliEntry != null && !brotliEntry.isDirectory()) {
			respond(request, contentType, zipArchive, brotliEntry, false, "br", maxAge, sha1);
		} else if (uncompressedEntry != null && !uncompressedEntry.isDirectory()) {
			if (allowGzip && uncompressedEntry.getCompressionMethod() == 8) {
				respond(request, contentType, zipArchive, uncompressedEntry, true, null, maxAge, sha1);
			} else {
				respond(request, contentType, zipArchive, uncompressedEntry, false, null, maxAge, sha1);
			}
		}
	}

	private void respond(HTTPRequest request, String contentType, ZipArchive zipArchive, ZipArchiveEntry entry, boolean convertDeflateToGzip, String encoding, long maxAge, String fileSha) throws Exception {
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
		if (contentType != null) {
			request.response.setHeader("Content-Type", contentType);
		}
		if (encoding != null) {
			request.response.setHeader("Content-Encoding", encoding);
		}
		request.response.contentLength(convertDeflateToGzip ? (entry.getCompressedLength() + 18L) : entry.getUncompressedLength());
		if (maxAge > 0L) {
			request.response.cache(maxAge);
			request.response.setHeader("Vary", "Accept-Encoding");
		} else {
			request.response.doNotCache();
		}
		if (convertDeflateToGzip) {
			request.response.write(gzipHeader);
		}
		Util.copy(entry.getInputStream(convertDeflateToGzip), request.response);
		if (convertDeflateToGzip) {
			byte[] gzipFooter = new byte[8];
			int crc32 = entry.getCrc();
			gzipFooter[0] = (byte) (crc32 & 0xff);
			gzipFooter[1] = (byte) ((crc32 >> 8) & 0xff);
			gzipFooter[2] = (byte) ((crc32 >> 16) & 0xff);
			gzipFooter[3] = (byte) ((crc32 >> 24) & 0xff);
			int uncompressedSize = (int) entry.getUncompressedLength();
			gzipFooter[4] = (byte) (uncompressedSize & 0xff);
			gzipFooter[5] = (byte) ((uncompressedSize >> 8) & 0xff);
			gzipFooter[6] = (byte) ((uncompressedSize >> 16) & 0xff);
			gzipFooter[7] = (byte) ((uncompressedSize >> 24) & 0xff);
			request.response.write(gzipFooter);
		}
	}

	public InputStream getFileStream(String file) throws IOException {
		ZipArchive zipArchive = updateAndGet(() -> archive);
		Properties properties = getProperties();
		String rename = properties.getProperty("rename:" + file);
		if (rename != null) {
			file = rename;
		}
		ZipArchiveEntry entry = zipArchive.getEntry(file);
		if (entry == null) {
			throw new FileNotFoundException(zipFile.getPath() + ":" + file);
		}
		return entry.getInputStream();
	}

	private <T> T updateAndGet(Supplier<T> getter) {
		archiveReadLock.lock();
		try {
			long lm = zipFile.lastModified();
			if (lastModified == lm) {
				return getter.get();
			}
		} finally {
			archiveReadLock.unlock();
		}
		archiveWriteLock.lock();
		try {
			long lm = zipFile.lastModified();
			if (lastModified == lm) {
				return getter.get();
			}
			ZipArchive zipArchive = ZipArchive.from(zipFile);
			try {
				props = getProperties(zipArchive);
				archive = zipArchive;
				lastModified = lm;
			} catch (Exception e) {
			}
			return getter.get();
		} finally {
			archiveWriteLock.unlock();
		}
	}

	public Properties getProperties() {
		return updateAndGet(() -> props);
	}

	private Properties getProperties(ZipArchive zipArchive) throws IOException {
		Properties properties = new Properties();
		properties.load(zipArchive.getEntry("info.txt").getInputStream());
		return properties;
	}

	private static List<String> getParents(String file) {
		List<String> parents = new ArrayList<>();
		do {
			int slashPos = file.lastIndexOf("/");
			if (slashPos == -1) slashPos = 0;
			file = file.substring(0, slashPos);
			parents.add(file);
		} while (!file.equals(""));
		return parents;
	}

	private static String getOption(String file, Properties props, String option) {
		for (String parent : getParents(file)) {
			String value = props.getProperty("option:" + parent + ":" + option);
			if (value != null) return value;
		}
		return props.getProperty(option);
	}
}
