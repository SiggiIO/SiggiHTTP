package io.siggi.http;

import io.siggi.http.defaultresponders.DefaultResponder;
import io.siggi.http.iphelper.IP;
import io.siggi.http.registry.HTTPResponderRegistry;
import io.siggi.http.session.Sessions;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class HTTPServer {

	private static final String version;
	private static final boolean snapshot;

	static {
		String ver = "UNKNOWN";
		boolean snap = false;
		try {
			InputStream pomProps = HTTPServer.class.getResourceAsStream("/META-INF/maven/io.siggi/SiggiHTTP/pom.properties");
			Properties props = new Properties();
			props.load(pomProps);
			ver = props.getProperty("version");
		} catch (Exception e) {
			// unable to determine version! :o
		}
		if (ver.endsWith("-SNAPSHOT")) {
			ver = ver.substring(0, ver.length() - 9);
			snap = true;
		}
		version = ver;
		snapshot = snap;
	}

	/**
	 * Get the SiggiHTTP version.
	 *
	 * @return server version
	 */
	public static String getVersion() {
		return version;
	}

	/**
	 * See if this is a snapshot version of SiggiHTTP.
	 *
	 * @return
	 */
	public static boolean isSnapshot() {
		return snapshot;
	}

	HTTPServer(Sessions sessions) {
		this.port = -1;
		startedProcessing = true;
		if (sessions != null) {
			this.sessions = sessions;
		}
	}

	/**
	 * Handle the socket. This method will start a new Thread.
	 *
	 * @param socket The Socket to use
	 * @throws java.io.IOException if something goes wrong
	 */
	public void handle(Socket socket) throws IOException {
		handle(socket, null);
	}

	/**
	 * Handle the socket, you are able to pass an InputStream containing bytes
	 * you already read from the socket to, for example, determine the protocol
	 * used on the connection, and since you determined that it's HTTP, you pass
	 * it here.
	 *
	 * @param socket The Socket to use
	 * @param preRead The InputStream containing bytes you already read
	 * @throws java.io.IOException if something goes wrong
	 */
	public void handle(Socket socket, InputStream preRead) throws IOException {
		if (!startedProcessing) {
			startedProcessing = true;
		}
		new HTTPHandler(this, socket, preRead).start();
	}

	/**
	 * Creates an <code>HTTPServer</code> and binds it to the specified port.
	 *
	 * @param port the port to listen on
	 * @throws IOException if something goes wrong creating the socket
	 */
	@Deprecated
	public HTTPServer(int port) throws IOException {
		this.port = port;
		serverSocket = new ServerSocket(port);
	}

	/**
	 * Creates an <code>HTTPServer</code> and binds it to the specified local
	 * port number, with the specified backlog.
	 *
	 * @param port the port to listen on
	 * @param backlog the backlog for the socket
	 * @throws IOException if something goes wrong creating the socket
	 */
	@Deprecated
	public HTTPServer(int port, int backlog) throws IOException {
		this.port = port;
		serverSocket = new ServerSocket(port, backlog);
	}

	/**
	 * Creates an <code>HTTPServer</code> with the specified port, listen
	 * backlog, and local IP address to bind to.
	 *
	 * @param port the port to listen on
	 * @param backlog the backlog for the socket
	 * @param bindAddr the address to bind to
	 * @throws IOException if something goes wrong creating the socket
	 */
	@Deprecated
	public HTTPServer(int port, int backlog, InetAddress bindAddr) throws IOException {
		this.port = port;
		serverSocket = new ServerSocket(port, backlog, bindAddr);
	}

	/**
	 * Creates an <code>HTTPServer</code> that will use the specified
	 * <code>ServerSocket</code> for incoming connections. This is useful for
	 * accepting HTTP connections using SSL, since HTTPServer doesn't have the
	 * ability to create SSL sockets by itself.
	 *
	 * @param serverSocket the ServerSocket to accept connections on
	 * @throws IOException if something goes wrong creating the socket
	 */
	@Deprecated
	public HTTPServer(ServerSocket serverSocket) throws IOException {
		int p = serverSocket.getLocalPort();
		if (p == -1) {
			throw new IOException("Bind ServerSocket before using HTTPServer!");
		}
		this.port = p;
		this.serverSocket = serverSocket;
	}

	/**
	 * Starts the listener thread.
	 */
	@Deprecated
	public void listen() {
		if (port == -1) {
			throw new IllegalStateException("This server was started without a ServerSocket.");
		}
		if (!listening) {
			listening = true;
			startedProcessing = true;
			new Thread(() -> {
				while (!killed) {
					try {
						new HTTPHandler(HTTPServer.this, serverSocket.accept(), serverSocket).start();
					} catch (IOException ioe) {
					}
				}
			}, "HTTPServer-SocketAcceptor").start();
		}
	}

	/**
	 * Adds an extra ServerSocket to accept connections from.
	 *
	 * @param extraSocket
	 */
	@Deprecated
	public void listen(ServerSocket extraSocket) {
		if (serverSockets.contains(extraSocket)) {
			return;
		}
		if (killed) {
			return;
		}
		serverSockets.add(extraSocket);
		startedProcessing = true;
		new Thread(() -> {
			while (!killed) {
				try {
					new HTTPHandler(HTTPServer.this, extraSocket.accept(), extraSocket).start();
				} catch (IOException ioe) {
				}
			}
		}, "HTTPServer-SecondaryAcceptor").start();
	}

	/**
	 * Kills HTTP Server
	 */
	@Deprecated
	public void kill() {
		if (killed) {
			return;
		}
		killed = true;
		try {
			serverSocket.close();
		} catch (Exception e) {
		}
		for (ServerSocket server : serverSockets) {
			try {
				server.close();
			} catch (Exception e) {
			}
		}
	}
	@Deprecated
	private boolean killed = false;
	/**
	 * The port that this HTTPServer is running on
	 */
	@Deprecated
	public final int port;
	@Deprecated
	private ServerSocket serverSocket;
	@Deprecated
	private final ArrayList<ServerSocket> serverSockets = new ArrayList<>();
	@Deprecated
	private boolean listening = false;
	@Deprecated
	private boolean startedProcessing = false;

	HTTPResponder respond404 = null;

	String serverName = "SiggiHTTP";

	/**
	 * Changes the server name in the Server response header.
	 *
	 * @param serverName the new server name
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	String getServerSignature(String host) {
		return "SiggiHTTP " + getVersion() + (isSnapshot() ? " (snapshot)" : "") + ", an HTTP server by Siggi - made with &hearts; on Planet Earth!";
	}

	/**
	 * The <code>HTTPResponder</code> registration records. Register
	 * <code>HTTPResponder</code>s into the virtual file system here. If you're
	 * running multiple websites, create a new HTTPResponderRegistry object, and
	 * use setResponderRegistry to use a specific registry for a specific
	 * hostname.
	 */
	public final HTTPResponderRegistry responderRegistry = new HTTPResponderRegistry();
	private final Map<String, HTTPResponderRegistry> responderRegistryMap = new HashMap<>();

	/**
	 * This will return the responder registry for the specified host name.
	 *
	 * @param hostname
	 * @return
	 */
	public final HTTPResponderRegistry getResponderRegistry(String hostname) {
		if (hostname.endsWith(":" + port)) {
			hostname = hostname.substring(0, hostname.lastIndexOf(":"));
		}
		HTTPResponderRegistry httprr = responderRegistryMap.get(hostname.toLowerCase());
		if (httprr == null) {
			httprr = responderRegistry;
		}
		return httprr;
	}

	/**
	 * This will point the specified hostname to the specified registry.
	 *
	 * @param hostname the hostname to set the registry for
	 * @param registry the registry to set for the hostname
	 */
	public final void setResponderRegistry(String hostname, HTTPResponderRegistry registry) {
		if (hostname == null || registry == null) {
			throw new NullPointerException();
		}
		if (hostname.endsWith(":" + port)) {
			hostname = hostname.substring(0, hostname.lastIndexOf(":"));
		}
		if (registry == responderRegistry) {
			responderRegistryMap.remove(hostname.toLowerCase());
		} else {
			responderRegistryMap.put(hostname.toLowerCase(), registry);
		}
	}

	/**
	 * Sets the responder when other responders throw a 404 error. (Not Found)
	 *
	 * @param responder set the default 404 responder
	 */
	public void set404Responder(HTTPResponder responder) {
		respond404 = responder;
	}
	private final Map<String, String> mimeTypes = new HashMap<>();

	/**
	 * Sets the mime type of files with the specified extension to the specified
	 * mime type.
	 *
	 * @param extension the extension to set the mime type for
	 * @param mimeType the mime type for the extension
	 */
	public void setMimeType(String extension, String mimeType) {
		extension = extension.toLowerCase();
		if (mimeType == null) {
			mimeTypes.remove(extension);
		} else {
			mimeTypes.put(extension, mimeType);
		}
	}

	/**
	 * Returns the mime type of the specified file extension.
	 *
	 * @param extension the file extension
	 * @return the mime type
	 */
	public String getMimeType(String extension) {
		String s = extension.toLowerCase();
		String mT = mimeTypes.get(extension);
		if (mT != null) {
			return mT;
		}
		if (s.equals("mp3")) {
			return "audio/mpeg3";
		}
		if (s.equals("aif") || s.equals("aiff")) {
			return "audio/x-aiff";
		}
		if (s.equals("aifc")) {
			return "audio/x-aifc";
		}
		if (s.equals("html") || s.equals("htm")) {
			return "text/html";
		}
		if (s.equals("txt")) {
			return "text/plain";
		}
		if (s.equals("swf")) {
			return "application/x-shockwave-flash";
		}
		if (s.equals("avi")) {
			return "video/x-msvideo";
		}
		if (s.equals("m3u")) {
			return "audio/x-mpegurl";
		}
		if (s.equals("m4v")) {
			return "video/x-m4v";
		}
		if (s.equals("m4a")) {
			return "audio/mp4a-latm";
		}
		if (s.equals("m4b")) {
			return "audio/mp4a-latm";
		}
		if (s.equals("m4p")) {
			return "audio/mp4a-latm";
		}
		if (s.equals("m4r")) {
			return "audio/mp4a-latm";
		}
		if (s.equals("mid") || s.equals("midi")) {
			return "audio/midi";
		}
		if (s.equals("mov")) {
			return "video/quicktime";
		}
		if (s.equals("ogg")) {
			return "application/ogg";
		}
		if (s.equals("mp4")) {
			return "video/mp4";
		}
		if (s.equalsIgnoreCase("png")) {
			return "image/png";
		}
		if (s.equalsIgnoreCase("jpg") || s.equalsIgnoreCase("jpeg")) {
			return "image/jpeg";
		}
		if (s.equalsIgnoreCase("gif")) {
			return "image/gif";
		}
		if (s.equalsIgnoreCase("ttf")) {
			return "application/x-font-ttf";
		}
		if (s.equalsIgnoreCase("css")) {
			return "text/css";
		}
		if (s.equalsIgnoreCase("js")) {
			return "text/javascript";
		}
		if (s.equalsIgnoreCase("json")) {
			return "application/json";
		}
		if (s.equalsIgnoreCase("svg")) {
			return "image/svg+xml";
		}
		if (s.equalsIgnoreCase("pdf")) {
			return "application/pdf";
		}
		return "application/x-octet-stream";
	}

	private final Set<IP> trustedIPs = new HashSet<>();
	private final ReentrantReadWriteLock trustedIPLock = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock.ReadLock trustedIPRead = trustedIPLock.readLock();
	private final ReentrantReadWriteLock.WriteLock trustedIPWrite = trustedIPLock.writeLock();

	public boolean isIPTrusted(String ip) {
		trustedIPRead.lock();
		try {
			IP ipAddr = IP.getIP(ip);
			do {
				if (trustedIPs.contains(ipAddr)) {
					return true;
				}
				ipAddr = IP.getIP(ipAddr.getFirstIP().toString() + "/" + (ipAddr.getPrefixLength() - 1));
			} while (ipAddr.getPrefixLength() > 0);
			return false;
		} finally {
			trustedIPRead.unlock();
		}
	}

	public void trustIP(String ip) {
		trustedIPWrite.lock();
		try {
			trustedIPs.add(IP.getIP(ip));
		} finally {
			trustedIPWrite.unlock();
		}
	}

	public void untrustIP(String ip) {
		trustedIPWrite.lock();
		try {
			trustedIPs.remove(IP.getIP(ip));
		} finally {
			trustedIPWrite.unlock();
		}
	}

	private Sessions sessions = Sessions.create(3600000L);

	/**
	 * Get the Sessions object for this HTTPServer.
	 *
	 * @return the sessions object
	 */
	public Sessions getSessions() {
		return sessions;
	}

	/**
	 * Set the Sessions object for this HTTPServer.
	 *
	 * @param sessions
	 */
	public void setSessions(Sessions sessions) {
		if (startedProcessing) {
			throw new IllegalStateException("Sessions cannot be changed after starting.");
		}
		this.sessions = sessions;
	}

	String getSessionCookieName() {
		return "SessID";
	}

	private final Map<Integer, HTTPResponder> defaultResponders = new HashMap<>();

	private void fillDefaultResponders() {
		defaultResponders.putAll(DefaultResponder.getAll());
	}

	{
		fillDefaultResponders();
	}

	public HTTPResponder getDefaultHandler(int code) {
		HTTPResponder r = defaultResponders.get(code);
		if (r == null) {
			r = defaultResponders.get(500);
		}
		return r;
	}

	private int postLimit = 2097152;

	/**
	 * Get the post size limit. This applies to post data excluding file
	 * uploads. Default is 2 MB.
	 *
	 * @return the upload limit in bytes
	 */
	public int getPostLimit() {
		return postLimit;
	}

	/**
	 * Set the post size limit. This applies to post data excluding file
	 * uploads. Default is 2 MB.
	 *
	 * @param postLimit the new post size upload limit
	 */
	public void setPostLimit(int postLimit) {
		this.postLimit = postLimit;
	}

	private long uploadLimit = 2147483648L;

	/**
	 * Get the file size upload limit. Default is 2 GB.
	 *
	 * @return the upload limit in bytes
	 */
	public long getUploadLimit() {
		return uploadLimit;
	}

	/**
	 * Set the file size upload limit. Default is 2 GB. This applies to the
	 * entire multipart/form-data content, including non-file content, if you
	 * need to upload files that are exactly a certain size, give few kb of
	 * extra headroom in addition the size of files you expect to receive.
	 *
	 * @param uploadLimit the new upload limit
	 */
	public void setUploadLimit(long uploadLimit) {
		this.uploadLimit = uploadLimit;
	}

	private int headerSizeLimit = 16384;

	/**
	 * Get the max size of headers. Default is 16 kB.
	 *
	 * @return max size of headers
	 */
	public int getHeaderSizeLimit() {
		return headerSizeLimit;
	}

	/**
	 * Set the max size of headers. Default is 16 kB.
	 *
	 * @param headerSizeLimit the new header size limit
	 */
	public void setHeaderSizeLimit(int headerSizeLimit) {
		this.headerSizeLimit = headerSizeLimit;
	}

	private int requestURISizeLimit = 16384;

	/**
	 * Get the max size of the Request URI line. Default is 16 kB.
	 *
	 * @return the max size of the request URI line
	 */
	public int getRequestURISizeLimit() {
		return requestURISizeLimit;
	}

	/**
	 * Set the max size of the request URI line. Default is 16 kB.
	 *
	 * @param requestURISizeLimit the new request URI size limit
	 */
	public void setRequestURISizeLimit(int requestURISizeLimit) {
		this.requestURISizeLimit = requestURISizeLimit;
	}

	private int websocketMaxPayloadLength = 16384;

	/**
	 * Get the max size of a websocket payload. Default is 16 kB.
	 *
	 * @return the max size of a websocket payload
	 */
	public int getWebsocketMaxPayloadLength() {
		return websocketMaxPayloadLength;
	}

	/**
	 * Set the max size of a websocket payload. Default is 16 kB.
	 *
	 * @param websocketMaxPayloadLength the new websocket payload size limit
	 */
	public void setWebsocketMaxPayloadLength(int websocketMaxPayloadLength) {
		this.websocketMaxPayloadLength = websocketMaxPayloadLength;
	}
}
