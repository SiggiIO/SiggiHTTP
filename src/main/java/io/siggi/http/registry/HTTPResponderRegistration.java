package io.siggi.http.registry;

import io.siggi.http.HTTPRequest;
import io.siggi.http.HTTPResponder;
import java.io.IOException;

public class HTTPResponderRegistration {

	HTTPResponderRegistration(String path, boolean endSlash, HTTPResponder responder, boolean includeSubpath, boolean caseSensitive) {
		this.path = path;
		this.endSlash = endSlash;
		this.responder = responder;
		this.includeSubpath = includeSubpath;
		this.caseSensitive = caseSensitive;
	}
	/**
	 * The path that is registered with this registration. Slashes at the end
	 * are removed.
	 */
	public final String path;
	final boolean endSlash;
	/**
	 * The <code>HTTPResponder</code> object that this registration links to.
	 */
	public final HTTPResponder responder;
	/**
	 * Specifies whether paths other than this registration's <code>path</code>
	 * that begin with this registration's <code>path</code> will match.
	 *
	 * @deprecated This field will be removed in a future version.
	 */
	@Deprecated
	public final boolean includeSubpath;
	/**
	 * Specifies whether this registration is case sensitive.
	 *
	 * @deprecated This field will be removed in a future version.
	 */
	@Deprecated
	public final boolean caseSensitive;
	/**
	 * An <code>HTTPResponder</code> that will forward the client to the
	 * <code>HTTPResponder</code> linked to this registration.
	 */
	@Deprecated
	public final HTTPResponder forwardHere = new HTTPResponder() {
		@Override
		public void respond(HTTPRequest request) throws IOException {
			request.response.setHeader("302 Found");
			request.response.setHeader("Location", path + ((includeSubpath || endSlash) ? "" : "/"));
			request.response.contentLength(0);
			request.response.setHeader("Content-Type", "text/plain");
			request.response.write(new byte[0]);
		}

		@Override
		public void respond404(HTTPRequest request) {
		}
	};
}
