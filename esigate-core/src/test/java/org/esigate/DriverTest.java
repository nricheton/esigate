/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.esigate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.esigate.events.EventManager;
import org.esigate.http.HttpClientHelper;
import org.esigate.http.MockHttpClient;
import org.esigate.tags.BlockRenderer;
import org.esigate.tags.TemplateRenderer;
import org.esigate.test.TestUtils;

public class DriverTest extends TestCase {
	private HttpEntityEnclosingRequest request;

	@Override
	protected void setUp() throws Exception {
		MockDriver provider = new MockDriver("mock");
		provider.addResource("/testBlock", "abc some<!--$beginblock$A$-->some text goes here<!--$endblock$A$--> cdf hello");
		provider.addResource("/testTemplateFullPage", "some <!--$beginparam$key$-->some hidden text goes here<!--$endparam$key$--> printed");
		provider.addResource("/testTemplate", "abc some<!--$begintemplate$A$-->some text goes here<!--$endtemplate$A$--> cdf hello");
		request = TestUtils.createRequest();
	}

	public void testRenderBlock() throws IOException, HttpErrorPage {
		Writer out = new StringWriter();
		DriverFactory.getInstance("mock").render("/testBlock", null, out, request, new BlockRenderer("A", "/testBlock"));

		assertEquals("some text goes here", out.toString());

		out = new StringWriter();
		DriverFactory.getInstance("mock").render("$(vartestBlock)", null, out, request, new BlockRenderer("A", "$(vartestBlock)"));
		assertEquals("some text goes here", out.toString());

		out = new StringWriter();
		DriverFactory.getInstance("mock").render("/$(vartest)$(varBlock)", null, out, request, new BlockRenderer("A", "/$(vartest)$(varBlock)"));
		assertEquals("some text goes here", out.toString());

	}

	public void testRenderTemplateFullPage() throws IOException, HttpErrorPage {
		StringWriter out = new StringWriter();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("key", "'value'");
		params.put("some other key", "'another value'");
		DriverFactory.getInstance("mock").render("/testTemplateFullPage", null, out, request, new TemplateRenderer(null, params, "/testTemplateFullPage"));
		assertFalse(out.toString().contains("key"));
		assertTrue(out.toString().contains("'value'"));
		assertFalse(out.toString().contains("some other key"));
		assertEquals("some 'value' printed", out.toString());
	}

	public void testRenderTemplate() throws IOException, HttpErrorPage {
		StringWriter out = new StringWriter();
		DriverFactory.getInstance("mock").render("/testTemplate", null, out, request, new TemplateRenderer("A", null, "/testTemplate"));
		assertEquals("some text goes here", out.toString());

		out = new StringWriter();
		DriverFactory.getInstance("mock").render("/test$(varTemplate)", null, out, request, new TemplateRenderer("A", null, "/test$(varTemplate)"));
		assertEquals("some text goes here", out.toString());

	}

	/** Test default configuration */
	public void testDefaultConfig() {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		Driver driver = new Driver("dummy", properties);
		// Parsable contentTypes
		assertParsableContentType(driver, "text/html; charset=utf-8");
		assertParsableContentType(driver, "application/xhtml+xml; charset=iso-8859-1");
		assertNotParsableContentType(driver, "application/octet-stream");
	}

	private void assertParsableContentType(Driver driver, String contentType) {
		assertTrue("Content-type should be considered as text", driver.isTextContentType(contentType));
	}

	private void assertNotParsableContentType(Driver driver, String contentType) {
		assertFalse("Content-type should be considered as binary", driver.isTextContentType(contentType));
	}

	/**
	 * Test property parsableContentTypes
	 */
	public void testParsableContentTypes() {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		properties.put(Parameters.PARSABLE_CONTENT_TYPES.name, "text/plain");
		Driver driver = new Driver("dummy", properties);
		assertParsableContentType(driver, "text/plain");

		properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		properties.put(Parameters.PARSABLE_CONTENT_TYPES.name, "text/plain, text/html");
		driver = new Driver("dummy", properties);
		assertParsableContentType(driver, "text/plain");
		assertParsableContentType(driver, "text/html");

		properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		properties.put(Parameters.PARSABLE_CONTENT_TYPES.name, "text/plain, text/html,application/x");
		driver = new Driver("dummy", properties);
		assertParsableContentType(driver, "text/plain");
		assertParsableContentType(driver, "text/html");
		assertParsableContentType(driver, "application/x");
	}

	public void testHeadersPreservedWhenError500() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		MockHttpClient mockHttpClient = new MockHttpClient();
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 500, "Internal Server Error");
		response.addHeader("Content-type", "Text/html;Charset=UTF-8");
		response.addHeader("Dummy", "dummy");
		HttpEntity httpEntity = new StringEntity("Error", "UTF-8");
		response.setEntity(httpEntity);
		mockHttpClient.setResponse(response);
		Driver driver = createMockDriver(properties, mockHttpClient);
		try {
			driver.proxy("/", request);
			fail("We should get an HttpErrorPage");
		} catch (HttpErrorPage e) {
			TestUtils.sendHttpErrorPage(e, request);
		}
		assertEquals("Status code", 500, TestUtils.getResponse(request).getStatusLine().getStatusCode());
		assertTrue("Header 'Dummy'", TestUtils.getResponse(request).containsHeader("Dummy"));
	}

	public void testHeadersFilteredWhenError500() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		MockHttpClient mockHttpClient = new MockHttpClient();
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 500, "Internal Server Error");
		response.addHeader("Content-type", "Text/html;Charset=UTF-8");
		response.addHeader("Transfer-Encoding", "dummy");
		HttpEntity httpEntity = new StringEntity("Error", "UTF-8");
		response.setEntity(httpEntity);
		mockHttpClient.setResponse(response);
		Driver driver = createMockDriver(properties, mockHttpClient);
		try {
			driver.proxy("/", request);
			fail("We should get an HttpErrorPage");
		} catch (HttpErrorPage e) {
			TestUtils.sendHttpErrorPage(e, request);
		}
		assertEquals("Status code", 500, TestUtils.getResponse(request).getStatusLine().getStatusCode());
		assertFalse("Header 'Transfer-Encoding'", TestUtils.getResponse(request).containsHeader("Transfer-Encoding"));
	}

	public void testSpecialCharacterInErrorPage() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		MockHttpClient mockHttpClient = new MockHttpClient();
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 500, "Internal Server Error");
		response.addHeader("Content-type", "Text/html;Charset=UTF-8");
		HttpEntity httpEntity = new StringEntity("é", "UTF-8");
		response.setEntity(httpEntity);
		mockHttpClient.setResponse(response);
		Driver driver = createMockDriver(properties, mockHttpClient);
		try {
			driver.proxy("/", request);
			fail("We should get an HttpErrorPage");
		} catch (HttpErrorPage e) {
			TestUtils.sendHttpErrorPage(e, request);
		}
		assertEquals("é", TestUtils.getResponseBodyAsString(request));
	}

	public void testGzipErrorPage() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		MockHttpClient mockHttpClient = new MockHttpClient();
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 500, "Internal Server Error");
		response.addHeader("Content-type", "Text/html;Charset=UTF-8");
		response.addHeader("Content-encoding", "gzip");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		byte[] uncompressedBytes = "é".getBytes("UTF-8");
		gzos.write(uncompressedBytes, 0, uncompressedBytes.length);
		gzos.close();
		byte[] compressedBytes = baos.toByteArray();
		ByteArrayEntity httpEntity = new ByteArrayEntity(compressedBytes);
		httpEntity.setContentType("Text/html;Charset=UTF-8");
		httpEntity.setContentEncoding("gzip");
		response.setEntity(httpEntity);
		mockHttpClient.setResponse(response);
		Driver driver = createMockDriver(properties, mockHttpClient);
		try {
			driver.proxy("/", request);
			fail("We should get an HttpErrorPage");
		} catch (HttpErrorPage e) {
			TestUtils.sendHttpErrorPage(e, request);
		}
		assertEquals("é", TestUtils.getResponseBodyAsString(request));
	}

	private Driver createMockDriver(Properties properties, HttpClient httpClient) {
		HttpClientHelper httpClientHelper = new HttpClientHelper(new EventManager(), null, httpClient, properties);
		return new Driver("tested", properties, httpClientHelper);
	}
	
	public void testRewriteRedirectResponse() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://www.foo.com:8080/");
		request = TestUtils.createRequest("http://www.bar.com/foo/");
		MockHttpClient mockHttpClient = new MockHttpClient();
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_MOVED_TEMPORARILY, "Found");
		response.addHeader("Location", "http://www.foo.com:8080/somewhere/");
		mockHttpClient.setResponse(response);
		Driver driver = createMockDriver(properties, mockHttpClient);
		driver.proxy("/foo/", request);
		assertEquals("http://www.bar.com/somewhere/",  TestUtils.getResponse(request).getFirstHeader("Location").getValue());
	}

}
