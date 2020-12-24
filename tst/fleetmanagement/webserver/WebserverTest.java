package fleetmanagement.webserver;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.*;
import java.net.*;
import java.security.KeyStore;

import javax.net.ssl.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.frontend.languages.Languages;
import fleetmanagement.test.LicenceStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.IOUtils;
import org.junit.*;

import fleetmanagement.frontend.controllers.Templates;
import fleetmanagement.frontend.security.SecurityRole;
import fleetmanagement.frontend.security.webserver.*;
import fleetmanagement.frontend.webserver.*;
import fleetmanagement.test.SessionStub;
import gsp.configuration.LocalFiles;

public class WebserverTest {

	private static int PORT = 13579;
	private static int SSL_PORT = 13589;

	private static Webserver testedHttp;
	private static Webserver testedHttps;
	private static SessionStub session;
	private static TestScenarioPrefilled scenario = new TestScenarioPrefilled();
	private static Languages languages;

	@BeforeClass
	public static void setupClass() throws Exception {
		languages = new Languages(null, new LicenceStub());
		Templates.init(languages, null);
		session = new SessionStub();
		testedHttp = startWebserver(false, PORT);
		testedHttps = startWebserver(true, SSL_PORT);
	}

	private static Webserver startWebserver(boolean withTls, int port) throws Exception {
		Webserver result = new Webserver(port, withTls);
		ThreadLocalSessionFilter sessionFilter = new ThreadLocalSessionFilter((sid) -> session, languages);
		result.addRequestFilter(sessionFilter);
		result.addResponseFilter(sessionFilter);
		result.addExceptionMapper(new FrontendExceptionMapper(mock(NotificationService.class)));
		result.addMessageBodyWriter(new ModelAndViewWriter(session));
		result.addResourceFilterFactory(new SecurityAnnotationProcessor());
		result.addResource(new TestResource());
		result.addResource(new RestrictedTestResource());
		result.addResource(new UserRoleRequiredTestResource());
		result.start();
		return result;
	}

	@AfterClass
	public static void teardownClass() {
		testedHttp.stop();
		testedHttps.stop();
	}

	@After
	public void teardown() {
		session.getSecurityContext().isSecure = false;
		session.getSecurityContext().roles.clear();
	}

	@Test
	public void servesAddedResources() throws Exception {
		HttpURLConnection connection = getFromServer("test");
		assertEquals(200, connection.getResponseCode());
		assertEquals("hello world", readContent(connection.getInputStream()));
	}

	@Test
	public void uses404Handler() throws Exception {
		HttpURLConnection connection = getFromServer("unknown");
		assertEquals(404, connection.getResponseCode());
	}

	@Test
	public void servesJson() throws Exception {
		HttpURLConnection connection = getFromServer("get-json");
		assertEquals(200, connection.getResponseCode());
		assertEquals("{\"foo\":\"bar\",\"baz\":1}", readContent(connection.getInputStream()));
	}

	@Test
	public void consumesJson() throws Exception {
		HttpURLConnection connection = postToServer("post-json", "{\"foo\":\"foo\",\"baz\":1234}");
		assertEquals(200, connection.getResponseCode());
	}

	@Test
	public void restrictsAccessToResourcesRequiringLogin() throws Exception {
		HttpURLConnection connection = getFromServer("restricted");
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), connection.getResponseCode());

		session.getSecurityContext().isSecure = true;
		connection = getFromServer("restricted");
		assertEquals("restricted", readContent(connection.getInputStream()));
	}

	@Test
	public void restrictsAccessToWriteRestrictedResources() throws Exception {
		session.getSecurityContext().isSecure = true;

		HttpURLConnection connection = getFromServer("write-role-required");
		assertEquals(Status.FORBIDDEN.getStatusCode(), connection.getResponseCode());

		setUserRole(SecurityRole.User);
		assertEquals("written", readContent(getFromServer("write-role-required").getInputStream()));
		setUserRole(SecurityRole.Config);
		assertEquals("written", readContent(getFromServer("write-role-required").getInputStream()));
		setUserRole(SecurityRole.Admin);
		assertEquals("written", readContent(getFromServer("write-role-required").getInputStream()));
	}

	@Test
	public void restrictsAccessToConfigRoleResources() throws Exception {
		session.getSecurityContext().isSecure = true;

		HttpURLConnection connection = getFromServer("write-role-required");
		assertEquals(Status.FORBIDDEN.getStatusCode(), connection.getResponseCode());

		setUserRole(SecurityRole.User);
		assertEquals(Status.FORBIDDEN.getStatusCode(), connection.getResponseCode());
		setUserRole(SecurityRole.Config);
		assertEquals("written", readContent(getFromServer("write-role-required").getInputStream()));
		setUserRole(SecurityRole.Admin);
		assertEquals("written", readContent(getFromServer("write-role-required").getInputStream()));
	}

	@Test
	public void restrictsAccessToAdminRoleResources() throws Exception {
		session.getSecurityContext().isSecure = true;

		HttpURLConnection connection = getFromServer("write-role-required");
		assertEquals(Status.FORBIDDEN.getStatusCode(), connection.getResponseCode());
		setUserRole(SecurityRole.User);
		assertEquals(Status.FORBIDDEN.getStatusCode(), connection.getResponseCode());
		setUserRole(SecurityRole.Config);
		assertEquals(Status.FORBIDDEN.getStatusCode(), connection.getResponseCode());
		setUserRole(SecurityRole.Admin);
		assertEquals("written", readContent(getFromServer("write-role-required").getInputStream()));
	}

	private void setUserRole(SecurityRole role) {
		session.getSecurityContext().roles.clear();
		session.getSecurityContext().roles.add(role.name());
	}

	@Test
	public void allowsManyParallelRequests() throws Exception {
		int numSockets = 100;
		long start = System.nanoTime();
		Socket[] sockets = startParallelRequests(numSockets);
		long diff = System.nanoTime() - start;
		assertTrue(String.format("requests needed too long: %.2fs", diff / 1E9), diff < 0.9 * 1E9);

		assertRequestsAreAllAnswered(numSockets, sockets);
		diff = System.nanoTime() - start;
		assertTrue(String.format("response time was too slow: %.2fs", diff / 1E9), diff < 2 * 1E9);
	}

	@Test
	public void allowsEncryptedConnection() throws Exception {
		HttpsURLConnection connection = getFromServerViaHttps("test");
		setupCertificates(connection);
		connection.getContent();
	}

	@Test(expected=SSLException.class)
	public void deniesUnauthorizedClientsOnEncryptedConnection() throws Exception {
		HttpsURLConnection connection = getFromServerViaHttps("test");
		connection.getContent();
	}

	@Test
	public void reportsServerHealth() throws Exception {
		HttpURLConnection connection = getFromServer("monitoring/health");
		assertEquals(200, connection.getResponseCode());
		assertEquals("200 OK", readContent(connection.getInputStream()));

		connection = getFromServer("health");
		assertEquals(200, connection.getResponseCode());
		assertEquals("200 OK", readContent(connection.getInputStream()));

		connection = getFromServer("monitoring/health");
		assertEquals(200, connection.getResponseCode());
		assertEquals("200 OK", readContent(connection.getInputStream()));
	}

	private void assertRequestsAreAllAnswered(int numSockets, Socket[] sockets) throws IOException {
		for (int i = 0; i < numSockets; i++) {
			assertTrue(sockets[i].isConnected());
			BufferedReader r = new BufferedReader(new InputStreamReader(sockets[i].getInputStream()));
			assertEquals("HTTP/1.1 200 OK", r.readLine());
			sockets[i].close();
		}
	}

	private Socket[] startParallelRequests(int count) throws IOException {
		Socket[] sockets = new Socket[count];
		for (int i = 0; i < count; i++) {
			sockets[i] = new Socket("localhost", PORT);
			BufferedWriter w = new BufferedWriter(new OutputStreamWriter(sockets[i].getOutputStream()));
			w.write("GET /longpoll HTTP/1.1\r\nHost: localhost\r\n\r\n");
			w.flush();
		}
		return sockets;
	}

	private HttpURLConnection postToServer(String relPath, String json) throws Exception {
		URL url = new URL("http://localhost:" + PORT + "/" + relPath);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		connection.setDoOutput(true);
		IOUtils.write(json, connection.getOutputStream());
		connection.getOutputStream().close();
		return connection;
	}

	private HttpURLConnection getFromServer(String relPath) throws Exception {
		URL url = new URL("http://localhost:" + PORT + "/" + relPath);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("GET");
		return connection;
	}

	private HttpsURLConnection getFromServerViaHttps(String relPath) throws Exception {
		URL url = new URL("https://localhost:" + SSL_PORT + "/" + relPath);
		HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
		connection.setRequestMethod("GET");
		return connection;
	}

	private String readContent(InputStream input) throws IOException {
		try (InputStream in = input) {
			return IOUtils.toString(in);
		}
	}

	private void setupCertificates(HttpsURLConnection connection) throws Exception {
		connection.setHostnameVerifier((h, s) -> true);

		final KeyStore keyStore = loadKeystore("client-certificates/keystore.jks", "hd84w19!".toCharArray());
		final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, "hd84w19!".toCharArray());

		final KeyStore trustStore = loadKeystore("client-certificates/truststore.jks", null);
		final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);

		final SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());
		final SSLSocketFactory socketFactory = sc.getSocketFactory();
		connection.setSSLSocketFactory(socketFactory);
	}

	private KeyStore loadKeystore(String pathToKeystore, char[] keystorePassword) throws Exception {
		final KeyStore keyStore = KeyStore.getInstance("JKS");
		try (final InputStream is = new FileInputStream(LocalFiles.find(pathToKeystore))) {
			keyStore.load(is, keystorePassword);
		}
		return keyStore;
	}

	@Path("/restricted")
	public static class RestrictedTestResource {
		@GET
		public String getRestrictedContent() {
			return "restricted";
		}
	}

	@Path("/write-role-required")
	@UserRoleRequired
	public static class UserRoleRequiredTestResource {
		@GET
		public String getRestrictedContent() {
			return "written";
		}
	}

	@Path("/config-role-required")
	@ConfigRoleRequired
	public static class ConfigRoleRequiredTestResource {
		@GET
		public String getRestrictedContent() {
			return "written";
		}
	}

	@Path("/admin-role-required")
	@AdminRoleRequired
	public static class AdminRoleRequiredTestResource {
		@GET
		public String getRestrictedContent() {
			return "written";
		}
	}

	@Path("/")
	@GuestAllowed
	public static class TestResource {
		@Path("test")
		@GET
		public String getString() {
			return "hello world";
		}

		@Path("get-json")
		@GET
		@Produces(MediaType.APPLICATION_JSON)
		public JsonContainer getJson() {
			return new JsonContainer();
		}

		@Path("post-json")
		@POST
		@Consumes(MediaType.APPLICATION_JSON)
		public Response postJson(JsonContainer input) {
			if (!input.foo.equals("foo") || input.baz != 1234)
				return Response.status(Status.BAD_REQUEST).build();
			return Response.ok().build();
		}


		@Path("longpoll")
		@GET
		public String getWithDelay() throws Exception {
			Thread.sleep(500);
			return "slow world";
		}
	}

	public static class JsonContainer {
		public String foo = "bar";
		public int baz = 1;
	}
}
