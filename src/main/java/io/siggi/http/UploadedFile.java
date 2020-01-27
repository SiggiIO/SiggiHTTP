package io.siggi.http;

import java.io.File;

/**
 * A file uploaded by the requesting client.  If the file is not moved away from it's temporary location, it will be deleted.
 */
public final class UploadedFile {
	UploadedFile(String filename, File file, String contentType) {
		if (filename == null || file == null) throw new NullPointerException();
		this.filename = filename;
		this.file = file;
		this.contentType = contentType;
	}
	/**
	 * The <code>java.io.File</code> linked to this <code>UploadedFile</code>.  If the file is not moved away from it's temporary location, it will be deleted.
	 */
	public final File file;
	/**
	 * The file's real name.
	 */
	public final String filename;
	/**
	 * The Content-Type that this file was uploaded with.
	 */
	public final String contentType;
	void delete() {
		if (file.exists()) file.delete();
	}
	
	@Override
	public boolean equals(Object other){
		if (!(other instanceof UploadedFile))return false;
		UploadedFile o = (UploadedFile) other;
		return equals(file, o.file) && equals(filename, o.filename) && equals(contentType, o.contentType);
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 89 * hash + (this.file != null ? this.file.hashCode() : 0);
		hash = 89 * hash + (this.filename != null ? this.filename.hashCode() : 0);
		hash = 89 * hash + (this.contentType != null ? this.contentType.hashCode() : 0);
		return hash;
	}
	
	private <K> boolean equals(K a, K b) {
		if (a == null) return b == null;
		if (b == null) return false;
		return a.equals(b);
	}
}
