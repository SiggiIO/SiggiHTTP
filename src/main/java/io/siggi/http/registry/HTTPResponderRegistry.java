package io.siggi.http.registry;

import io.siggi.http.HTTPResponder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use this to map paths to <code>HTTPResponder</code>s in the virtual
 * filesystem.
 */
public class HTTPResponderRegistry {

	public HTTPResponderRegistry() {
	}
	private final Map<String,HTTPResponderRegistration> registrationMap = new HashMap<>();
	private final List<HTTPResponderRegistration> registrations = new ArrayList<>();
	private ResponderRegistryI implementation = new ResponderRegistry(registrationMap);

	/**
	 * Registers an HTTPResponder.
	 * <p>
	 * All registrations include subdirectories.
	 * <p>
	 * A registration with a more specific path takes higher precedence than a
	 * registration with a less specific path. Registrations are case-sensitive.
	 */
	public HTTPResponderRegistration register(String path, HTTPResponder responder) {
		HTTPResponderRegistration httprrr = new HTTPResponderRegistration(path, path.endsWith("/"), responder, true, true);
		registrations.add(httprrr);
		registrationMap.put(path, httprrr);
		return httprrr;
	}

	/**
	 * Registers an HTTPResponder. It is important to note that calling this method will
	 * replace the responder registry implementation with the old deprecated implementation.
	 * <p>
	 * If <code>includeSubpath</code> is <code>true</code> then inner responders
	 * should be registered <b>first</b>.
	 * <p>
	 * For example, if you have a responder that responds to
	 * <code>/foo/bar/1234</code> and a responder that responds to
	 * <code>/foo</code> you should register <code>/foo/bar/1234</code>
	 * <b>before</b> registering <code>/foo</code>.
	 *
	 * @deprecated Will be removed in a future version - use {@link #register(String, HTTPResponder)} instead.
	 */
	@Deprecated
	public HTTPResponderRegistration register(String path, HTTPResponder responder, boolean includeSubpath, boolean caseSensitive) {
		if (!(implementation instanceof ResponderRegistryOld)) {
			implementation = new ResponderRegistryOld(registrations);
		}
		boolean endSlash = path.endsWith("/");
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		HTTPResponderRegistration httprrr = new HTTPResponderRegistration(path, endSlash, responder, includeSubpath, caseSensitive);
		registrations.add(httprrr);
		return httprrr;
	}

	/**
	 * Unregisters an HTTPResponder.
	 */
	public void unregister(HTTPResponderRegistration httprrr) {
		registrations.remove(httprrr);
		registrationMap.remove(httprrr.path, httprrr);
	}

	/**
	 * Finds a registration record that matches the specified path.
	 */
	public HTTPResponderRegistration getRecord(String path) {
		return implementation.getRecord(path);
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
}
