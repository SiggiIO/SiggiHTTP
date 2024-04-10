package io.siggi.http;

import io.siggi.http.session.Sessions;
import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public final class HTTPServerBuilder {

	private Sessions sessions;
	private String sessionCookieName;
	private File tmpDir;
	private Executor executor;

	public HTTPServerBuilder() {
		tmpDir = new File(System.getProperty("java.io.tmpdir"));
	}

	public HTTPServerBuilder setSessions(Sessions sessions) {
		this.sessions = sessions;
		return this;
	}

	public HTTPServerBuilder setSessionCookieName(String sessionCookieName) {
		this.sessionCookieName = sessionCookieName;
		return this;
	}

	public HTTPServerBuilder setTmpDir(File tmpDir) {
		this.tmpDir = tmpDir;
		return this;
	}

	public HTTPServerBuilder setExecutor(Executor executor) {
		this.executor = executor;
		return this;
	}

	public HTTPServer build() {
		return new HTTPServer(sessions, sessionCookieName, tmpDir, executor);
	}
}
