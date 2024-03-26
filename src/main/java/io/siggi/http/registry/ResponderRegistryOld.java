package io.siggi.http.registry;

import java.util.List;

class ResponderRegistryOld implements ResponderRegistryI {

	private final List<HTTPResponderRegistration> registrations;

	ResponderRegistryOld(List<HTTPResponderRegistration> registrations) {
		this.registrations = registrations;
	}

	@Override
	@SuppressWarnings("deprecation")
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
}
