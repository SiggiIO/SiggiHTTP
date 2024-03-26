package io.siggi.http.registry;

interface ResponderRegistryI {
	HTTPResponderRegistration getRecord(String path);
}
