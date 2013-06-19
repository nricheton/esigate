package org.esigate.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.mockito.Mockito;

/**
 * Fluent-style builder for mocked HttpServletRequest.
 * 
 * <p>
 * Default request is
 * 
 * <pre>
 * GET http://localhost/ HTTP/1.1
 * </pre>
 * 
 * @author Nicolas Richeton
 * 
 */
public class MockHttpServletRequestBuilder {
	String protocolVersion = "HTTP/1.1";
	String uriString = "http://localhost/";
	List<Header> headers = new ArrayList<Header>();
	HttpEntity entity = null;
	private String method = "GET";
	Map<String, Object> session = null;

	// boolean mockMediator = false;
	// ContainerRequestMediator mediator = null;

	public MockHttpServletRequestBuilder uri(String uri) {
		this.uriString = uri;
		return this;
	}

	/**
	 * Duplicate headers are not supported currently.
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public MockHttpServletRequestBuilder header(String name, String value) {
		this.headers.add(new BasicHeader(name, value));
		return this;
	}

	public MockHttpServletRequestBuilder method(String paramMethod) {
		this.method = paramMethod;
		return this;
	}

	public MockHttpServletRequestBuilder protocolVersion(String paramProtocolVersion) {
		this.protocolVersion = paramProtocolVersion;
		return this;
	}

	/**
	 * Session values are not yet supported. Null or existing object are
	 * honored.
	 * 
	 * @param paramSession
	 * @return
	 */
	public MockHttpServletRequestBuilder session(Map<String, Object> paramSession) {
		this.session = paramSession;
		return this;
	}

	// public MockHttpServletRequestBuilder entity(HttpEntity paramEntity) {
	// this.entity = paramEntity;
	// return this;
	// }

	// public MockHttpServletRequestBuilder mockMediator() {
	//
	// if (this.mediator != null)
	// throw new IllegalArgumentException(
	// "Cannot use both mockMediator and mediator when building HttpRequest");
	//
	// this.mockMediator = true;
	// return this;
	// }

	// public MockHttpServletRequestBuilder mediator(ContainerRequestMediator
	// paramMediator) {
	// if (this.mockMediator)
	// throw new IllegalArgumentException(
	// "Cannot use both mockMediator and mediator when building HttpRequest");
	//
	// this.mediator = paramMediator;
	// return this;
	// }

	/**
	 * Build the request as defined in the current builder.
	 * 
	 * @return the request
	 */
	public HttpServletRequest build() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

		Mockito.when(request.getMethod()).thenReturn(this.method);
		Mockito.when(request.getProtocol()).thenReturn(this.protocolVersion);
		Mockito.when(request.getRequestURI()).thenReturn(this.uriString);

		Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(headers));
		for (Header h : headers) {
			List<String> hresult = new ArrayList<String>();
			hresult.add(h.getValue());
			Mockito.when(request.getHeaders(h.getName())).thenReturn(Collections.enumeration(hresult));
			Mockito.when(request.getHeader(h.getName())).thenReturn(h.getValue());
		}

		if (session == null) {
			Mockito.when(request.getSession()).thenReturn(null);
		} else {
			HttpSession sessionMock = Mockito.mock(HttpSession.class);
			Mockito.when(request.getSession()).thenReturn(sessionMock);
		}
		return request;
	}
}
