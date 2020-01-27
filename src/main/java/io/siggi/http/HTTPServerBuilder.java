package io.siggi.http;

import io.siggi.http.session.Sessions;

public final class HTTPServerBuilder {

	private Sessions sessions;

	public HTTPServerBuilder() {
	}

	public HTTPServerBuilder setSessions(Sessions sessions) {
		this.sessions = sessions;
		return this;
	}

	public HTTPServer build() {
		return new HTTPServer(sessions);
	}
}
