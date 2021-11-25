package io.siggi.http.defaultresponders;

import io.siggi.http.HTTPRequest;
import io.siggi.http.HTTPResponder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The default responder.
 */
public class DefaultResponder implements HTTPResponder {

	public static final String STYLE =
			"<style>\n"
					+ "body { font-family: \"Calisto MT\", Optima, \"Lucida Bright\", serif; }\n"
					+ "td { font-family: \"Calisto MT\", Optima, \"Lucida Bright\", serif; }\n"
					+ "</style>\n";
	private final int code;
	private final String title, message;

	private DefaultResponder(int code) {
		this.code = code;
		ResponseCode a = codes.get(code);
		if (a == null) {
			title = "Error";
			message = "An error has occurred";
		} else {
			title = a.title;
			message = a.message;
		}
	}

	@Override
	public void respond(HTTPRequest request) throws IOException {
		String page = "<!DOCTYPE html>\n<html>\n<head>\n<title>" + code + " " + title + "</title>\n" + STYLE + "</head>\n<body>\n<h1>" + code + " " + title + "</h1><br>\n" + message + "<br>\n<hr>\n" + request.getServerSignature() + "<br>\n</body>\n</html>";
		byte pageBytes[] = request.response.getBytes(page);
		request.response.setHeader(code + " " + title);
		request.response.contentLength(pageBytes.length);
		request.response.setHeader("Content-Type", "text/html; charset=UTF-8");
		request.response.write(pageBytes);
	}

	@Override
	public void respond404(HTTPRequest request) throws IOException {
		respond(request);
	}

	private static class ResponseCode {

		private final int code;
		private final String title;
		private final String message;

		private ResponseCode(int code, String title, String message) {
			this.code = code;
			this.title = title;
			this.message = message;
		}
	}

	private static final Map<Integer, ResponseCode> codes = new HashMap<>();

	private static void add(int code, String title, String message) {
		codes.put(code, new ResponseCode(code, title, message));
	}

	static {
		add(400, "Bad Request", "I'm sorry, but I don't understand your request.");
		add(401, "Unauthorized", "I'm sorry, I can't let you access this without authorization.");
		add(402, "Payment Required", "Payment Required");
		add(403, "Forbidden", "I'm sorry, but this resource is off limits.");
		add(404, "Not Found", "The resource you're looking for isn't here lol, I'm sorry lol.");
		add(405, "Method Not Allowed", "The requested method is not allowed.");
		add(406, "Not Acceptable", "The request made by your browser is not acceptable by the server.");
		add(407, "Proxy Authentication Required", "The proxy needs to authenticate itself.");
		add(408, "Request Timeout", "Took too long to receive a request.");
		add(409, "Conflict", "Something went wrong! :/");
		add(410, "Gone", "What you're looking for is gone, probably forever! I'm sorry!");
		add(411, "Length Required", "Content-Length is required.");
		add(412, "Precondition Failed", "The server cannot meet requirements set by the client.");
		add(413, "Payload Too Large", "The request payload is too large.");
		add(414, "URI Too Long", "The URI is too long.");
		add(429, "Too Many Requests", "Please slow down, you're making me nervous!");
		add(451, "Unavailable For Legal Reasons", "Do you know the difference between illegal and unlawful? Well, unlawful is against the law, and illegal is a sick bird!");
		add(500, "Internal Server Error", "*puke* ugh, I feel sick.");
		add(501, "Not Implemented", "This method is not implemented");
		add(503, "Service Unavailable", "This service is not available at this time.");
	}

	public static Map<Integer, DefaultResponder> getAll() {
		Map<Integer, DefaultResponder> responders = new HashMap<>();
		for (Map.Entry<Integer, ResponseCode> entry : codes.entrySet()) {
			responders.put(entry.getKey(), new DefaultResponder(entry.getKey()));
		}
		return responders;
	}
}
