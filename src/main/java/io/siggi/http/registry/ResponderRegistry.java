package io.siggi.http.registry;

import java.util.Map;

class ResponderRegistry implements ResponderRegistryI {

	private final Map<String,HTTPResponderRegistration> registrations;

	ResponderRegistry(Map<String,HTTPResponderRegistration> registrations) {
		this.registrations = registrations;
	}

	@Override
	public HTTPResponderRegistration getRecord(String path) {
		do {
			HTTPResponderRegistration registration = registrations.get(path);
			if (registration != null) return registration;
			path = path.substring(0, path.lastIndexOf("/", path.length() - 2) + 1);
		} while (!path.isEmpty());
		return null;
	}
}
