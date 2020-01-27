package io.siggi.http;

/**
 * Page generators should implement this interface
 */
public interface HTTPResponder {

	/**
	 * Page generators should generate the full HTTP response including the HTTP
	 * response headers. (Yes that includes the very top header which indicates
	 * the status code)
	 *
	 * @param request the request to handle
	 * @throws Exception if something goes wrong
	 */
	public void respond(HTTPRequest request) throws Exception;

	/**
	 * Respond 404 here. If you don't want to use a custom 404 page, and use the
	 * server default instead, create a method that does nothing.
	 *
	 * @param request the request to handle
	 * @throws Exception if something goes wrong
	 */
	public void respond404(HTTPRequest request) throws Exception;
}
