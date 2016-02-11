package org.esigate.cas;

import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.esigate.Driver;
import org.esigate.DriverFactory;
import org.esigate.Parameters;
import org.esigate.cookie.CookieManager;
import org.esigate.events.EventManager;
import org.esigate.events.impl.FragmentEvent;
import org.esigate.extension.ExtensionFactory;
import org.esigate.http.BasicCloseableHttpResponse;
import org.esigate.http.HttpClientRequestExecutor;
import org.esigate.http.IncomingRequest;
import org.esigate.http.OutgoingRequest;
import org.esigate.impl.DriverRequest;
import org.esigate.test.TestUtils;
import org.esigate.test.conn.MockConnectionManager;
import org.esigate.test.http.HttpResponseBuilder;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.junit.Assert;

import junit.framework.TestCase;

/**
 * CasAuthenticationHandlerTest
 */
public class CasAuthenticationHandlerTest extends TestCase {
	private Driver driver1;
	private CasAuthenticationHandler handler;
	private HttpClientRequestExecutor httpClientRequestExecutor;
	private MockConnectionManager mockConnectionManager;

	private Driver createMockDriver(Properties properties, HttpClientConnectionManager connectionManager, String name) {
		Driver driver = Driver.builder().setName(name).setProperties(properties)
				.setRequestExecutorBuilder(HttpClientRequestExecutor.builder().setConnectionManager(connectionManager))
				.build();
		DriverFactory.put(name, driver);
		return driver;
	}

	private HttpResponse createMockResponse(String entity) throws Exception {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_OK, "OK");
		HttpEntity httpEntity = new StringEntity(entity);
		response.setEntity(httpEntity);
		return response;
	}

	@Override
	public void setUp() {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE, "http://localhost:8080");
		properties.put(CasAuthenticationHandler.CAS_LOGIN_URL, "/loginurl");

		mockConnectionManager = new MockConnectionManager();

		driver1 = Driver.builder().setName("driver1").setProperties(properties).setRequestExecutorBuilder(
				HttpClientRequestExecutor.builder().setConnectionManager(mockConnectionManager).setCookieManager(
						(CookieManager) ExtensionFactory.getExtension(properties, Parameters.COOKIE_MANAGER, null)))
				.build();

		httpClientRequestExecutor = (HttpClientRequestExecutor) driver1.getRequestExecutor();
		handler = new CasAuthenticationHandler();

		handler.init(driver1, properties);
	}

	public void testAutologin() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.getName(), "http://localhost:8080/jahia/");
		properties.put("casLoginUrl", "http://localhost:8081/cas/login");
		properties.put(Parameters.EXTENSIONS, CasAuthenticationHandler.class.getName());

		mockConnectionManager = new MockConnectionManager() {
			@Override
			public HttpResponse execute(HttpRequest httpRequest) {
				return new HttpResponseBuilder().status(HttpStatus.SC_MOVED_TEMPORARILY)
						.header("Location", "http://localhost:8081/cas/login").build();
			}
		};

		Driver driver = createMockDriver(properties, mockConnectionManager, "tested");

		IncomingRequest.Builder request = TestUtils.createIncomingRequest("http://127.0.0.1:8081/jahia/home");

		HttpResponse response = driver.proxy("/home", request.build());

		Assert.assertEquals(302, response.getStatusLine().getStatusCode());
		Assert.assertEquals("http://localhost:8081/cas/login?service=http%3A%2F%2F127.0.0.1%3A8081%2Fjahia%2Fhome",
				response.getFirstHeader("Location").getValue());
	}

	// public void testCasAuthenticationKo() throws Exception {
	// DriverRequest driverRequest = TestUtils.createDriverRequest(driver1);
	//
	// OutgoingRequest outgoingRequest =
	// httpClientRequestExecutor.createOutgoingRequest(driverRequest,
	// "http://localhost:8080", true);
	//
	// FragmentEvent event = new
	// FragmentEvent(driverRequest.getOriginalRequest(), outgoingRequest,
	// outgoingRequest.getContext());
	// CloseableHttpResponse httpResponse =
	// BasicCloseableHttpResponse.adapt(createMockResponse("0"));
	// httpResponse.setHeader("Location",
	// "http://localhost/loginurl?service=http");
	// event.setHttpResponse(httpResponse);
	//
	// HttpResponse responseOnceAuthenticated = createMockResponse("1");
	// mockConnectionManager.setResponse(responseOnceAuthenticated);
	//
	// handler.event(EventManager.EVENT_FRAGMENT_POST, event);
	//
	// // No extra request should be sent
	// assertNull(mockConnectionManager.getSentRequest());
	// // The response should be "unauthorized" as we cannot send the CAS
	// // ticket
	// assertEquals(401,
	// event.getHttpResponse().getStatusLine().getStatusCode());
	// }

	public void testCasAuthenticationOk() throws Exception {
		AttributePrincipal userPrincipal = new AttributePrincipal() {
			private static final long serialVersionUID = 1L;

			@Override
			public Map<String, Object> getAttributes() {
				return null;
			}

			@Override
			public String getName() {
				return "test";
			}

			@Override
			public String getProxyTicketFor(String arg0) {
				return "proxy_ticket";
			}
		};
		IncomingRequest incomingRequest = TestUtils.createIncomingRequest().setUserPrincipal(userPrincipal).build();
		DriverRequest driverRequest = new DriverRequest(incomingRequest, driver1, "/");
		;
		OutgoingRequest outgoingRequest = httpClientRequestExecutor.createOutgoingRequest(driverRequest,
				"http://localhost:8080", true);
		FragmentEvent event = new FragmentEvent(driverRequest.getOriginalRequest(), outgoingRequest,
				outgoingRequest.getContext());
		CloseableHttpResponse httpResponse = BasicCloseableHttpResponse.adapt(createMockResponse("0"));
		httpResponse.setHeader("Location", "http://localhost/loginurl?service=http");
		event.setHttpResponse(httpResponse);

		HttpResponse responseOnceAuthenticated = createMockResponse("1");
		mockConnectionManager.setResponse(responseOnceAuthenticated);

		handler.event(EventManager.EVENT_FRAGMENT_POST, event);

		// A new request should have been sent with the proxy ticket
		assertNotNull(mockConnectionManager.getSentRequest());
		assertEquals("/?ticket=proxy_ticket", mockConnectionManager.getSentRequest().getRequestLine().getUri());
		assertEquals(200, event.getHttpResponse().getStatusLine().getStatusCode());
		assertEquals("1", EntityUtils.toString(event.getHttpResponse().getEntity()));
	}

	public void testNoCasAuthenticationRequired() throws Exception {
		DriverRequest driverRequest = TestUtils.createDriverRequest(driver1);
		OutgoingRequest outgoingRequest = httpClientRequestExecutor.createOutgoingRequest(driverRequest,
				"http://localhost:8080", true);
		FragmentEvent event = new FragmentEvent(driverRequest.getOriginalRequest(), outgoingRequest,
				outgoingRequest.getContext());
		CloseableHttpResponse httpResponse = BasicCloseableHttpResponse.adapt(createMockResponse("0"));
		event.setHttpResponse(httpResponse);

		handler.event(EventManager.EVENT_FRAGMENT_POST, event);

		// No extra request should be sent
		assertNull(mockConnectionManager.getSentRequest());
	}

}
