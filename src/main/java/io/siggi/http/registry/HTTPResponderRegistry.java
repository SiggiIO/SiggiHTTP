package io.siggi.http.registry;

import io.siggi.http.HTTPResponder;
import io.siggi.http.HTTPWebSocketHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Use this to map paths to <code>HTTPResponder</code>s in the virtual
 * filesystem.
 */
public class HTTPResponderRegistry {

	public HTTPResponderRegistry() {
	}
	private final List<HTTPResponderRegistration> registrations = new ArrayList<>();
	private final List<HTTPWebSocketRegistration> wsRegistrations = new ArrayList<>();

	/**
	 * Registers an HTTPResponder.
	 * <p>
	 * If <code>includeSubpath</code> is <code>true</code> then inner responders
	 * should be registered <b>first</b>.
	 * <p>
	 * For example, if you have a responder that responds to
	 * <code>/foo/bar/1234</code> and a responder that responds to
	 * <code>/foo</code> you should register <code>/foo/bar/1234</code>
	 * <b>before</b> registering <code>/foo</code>.
	 */
	public HTTPResponderRegistration register(String path, HTTPResponder responder, boolean includeSubpath, boolean caseSensitive) {
		boolean endSlash = path.endsWith("/");
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		HTTPResponderRegistration httprrr = null;
		while ((httprrr = getRecord(path)) != null) {
			registrations.remove(httprrr);
		}
		httprrr = new HTTPResponderRegistration(path, endSlash, responder, includeSubpath, caseSensitive);
		registrations.add(httprrr);
		return httprrr;
	}

	/**
	 * Registers an HTTPResponder.
	 * <p>
	 * If <code>includeSubpath</code> is <code>true</code> then inner responders
	 * should be registered <b>first</b>.
	 * <p>
	 * For example, if you have a responder that responds to
	 * <code>/foo/bar/1234</code> and a responder that responds to
	 * <code>/foo</code> you should register <code>/foo/bar/1234</code>
	 * <b>before</b> registering <code>/foo</code>.
	 */
	public HTTPWebSocketRegistration registerWebSocketHandler(String path, HTTPWebSocketHandler handler, boolean includeSubpath, boolean caseSensitive) {
		boolean endSlash = path.endsWith("/");
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		HTTPWebSocketRegistration httprrr = null;
		while ((httprrr = getWebSocketRecord(path)) != null) {
			wsRegistrations.remove(httprrr);
		}
		httprrr = new HTTPWebSocketRegistration(path, endSlash, handler, includeSubpath, caseSensitive);
		wsRegistrations.add(httprrr);
		return httprrr;
	}

	/**
	 * Unregisters an HTTPResponder.
	 */
	public void unregister(HTTPResponderRegistration httprrr) {
		registrations.remove(httprrr);
	}

	/**
	 * Unregisters an HTTPWebSocketRegistration.
	 */
	public void unregister(HTTPWebSocketRegistration httprrr) {
		registrations.remove(httprrr);
	}

	/**
	 * Finds a registration record that matches the specified path.
	 */
	public HTTPResponderRegistration getRecord(String path) {
		boolean isDirectoryRequest = path.endsWith("/");
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		for (HTTPResponderRegistration responder : registrations) {
			String requestPath = path;
			String responderPath = responder.path;
			if (!responder.caseSensitive) {
				requestPath = requestPath.toLowerCase();
				responderPath = responderPath.toLowerCase();
			}
			if (requestPath.equals(responderPath) && (responder.includeSubpath || !isDirectoryRequest || responder.endSlash)) {
				return responder;
			}
			if (responder.includeSubpath) {
				if ((requestPath + "/").startsWith(responderPath + "/")) {
					return responder;
				}
			}
		}
		return null;
	}

	/**
	 * Finds a registration record that matches the specified path.
	 */
	public HTTPWebSocketRegistration getWebSocketRecord(String path) {
		boolean isDirectoryRequest = path.endsWith("/");
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		for (HTTPWebSocketRegistration responder : wsRegistrations) {
			String requestPath = path;
			String responderPath = responder.path;
			if (!responder.caseSensitive) {
				requestPath = requestPath.toLowerCase();
				responderPath = responderPath.toLowerCase();
			}
			if (requestPath.equals(responderPath) && (responder.includeSubpath || !isDirectoryRequest || responder.endSlash)) {
				return responder;
			}
			if (responder.includeSubpath) {
				if ((requestPath + "/").startsWith(responderPath + "/")) {
					return responder;
				}
			}
		}
		return null;
	}

	/**
	 * Finds a registered responder that matches the specified path.
	 */
	public HTTPResponder getResponder(String path) {
		HTTPResponderRegistration httprrr = getRecord(path);
		if (httprrr == null) {
			return null;
		}
		return httprrr.responder;
	}

	/**
	 * Finds a registered responder that matches the specified path.
	 */
	public HTTPWebSocketHandler getWebSocketHandler(String path) {
		HTTPWebSocketRegistration httprrr = getWebSocketRecord(path);
		if (httprrr == null) {
			return null;
		}
		return httprrr.handler;
	}
}
