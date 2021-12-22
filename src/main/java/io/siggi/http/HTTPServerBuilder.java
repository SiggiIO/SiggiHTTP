package io.siggi.http;

import io.siggi.http.session.Sessions;
import java.io.File;

public final class HTTPServerBuilder {

	private Sessions sessions;
	private File tmpDir;

	public HTTPServerBuilder() {
		tmpDir = new File(System.getProperty("java.io.tmpdir"));
	}

	public HTTPServerBuilder setSessions(Sessions sessions) {
		this.sessions = sessions;
		return this;
	}

	public HTTPServerBuilder setTmpDir(File tmpDir) {
		this.tmpDir = tmpDir;
		return this;
	}

	public HTTPServer build() {
		return new HTTPServer(sessions, tmpDir);
	}
}
