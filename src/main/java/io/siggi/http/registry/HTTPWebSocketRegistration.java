package io.siggi.http.registry;

import io.siggi.http.HTTPWebSocketHandler;

public class HTTPWebSocketRegistration {

	HTTPWebSocketRegistration(String path, boolean endSlash, HTTPWebSocketHandler handler, boolean includeSubpath, boolean caseSensitive) {
		this.path = path;
		this.endSlash = endSlash;
		this.handler = handler;
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
	 * The <code>HTTPWebSocketHandler</code> object that this registration links
	 * to.
	 */
	public final HTTPWebSocketHandler handler;
	/**
	 * Specifies whether paths other than this registration's <code>path</code>
	 * that begin with this registration's <code>path</code> will match.
	 */
	public final boolean includeSubpath;
	/**
	 * Specifies whether this registration is case sensitive.
	 */
	public final boolean caseSensitive;
	/**
	 * An <code>HTTPResponder</code> that will forward the client to the
	 * <code>HTTPResponder</code> linked to this registration.
	 */
}
