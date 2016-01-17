/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.esigate.http;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.esigate.impl.DriverRequest;
import org.esigate.impl.UrlRewriter;
import org.esigate.util.FilterList;
import org.esigate.util.UriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class is responsible for copying headers from incoming requests to outgoing requests and from incoming responses
 * to outgoing responses.
 * 
 * @author Francois-Xavier Bonnet
 * 
 */
public class HeaderManager {
    private static final Logger LOG = LoggerFactory.getLogger(HeaderManager.class);

    private final FilterList requestHeadersFilterList = new FilterList();
    private final FilterList responseHeadersFilterList = new FilterList();

    private final UrlRewriter urlRewriter;

    /**
     * Builds a Header manager.
     * 
     * @param urlRewriter
     *            The {@link UrlRewriter} to be used to rewrite headers like "Location"
     */
    public HeaderManager(UrlRewriter urlRewriter) {

        this.urlRewriter = urlRewriter;

        // Populate headers filter lists

        // By default all request headers are forwarded
        requestHeadersFilterList.add("*");
        // Except hop-by-hop headers
        requestHeadersFilterList.remove("Connection");
        requestHeadersFilterList.remove("Content-Length");
        requestHeadersFilterList.remove("Cache-control");
        requestHeadersFilterList.remove("Cookie");
        requestHeadersFilterList.remove("Host");
        requestHeadersFilterList.remove("Max-Forwards");
        requestHeadersFilterList.remove("Pragma");
        requestHeadersFilterList.remove("Proxy-Authorization");
        requestHeadersFilterList.remove("TE");
        requestHeadersFilterList.remove("Trailer");
        requestHeadersFilterList.remove("Transfer-Encoding");
        requestHeadersFilterList.remove("Upgrade");

        // By default all response headers are forwarded
        responseHeadersFilterList.add("*");
        // Except hop-by-hop headers
        responseHeadersFilterList.remove("Connection");
        responseHeadersFilterList.remove("Content-Length");
        responseHeadersFilterList.remove("Content-MD5");
        responseHeadersFilterList.remove("Date");
        responseHeadersFilterList.remove("Keep-Alive");
        responseHeadersFilterList.remove("Proxy-Authenticate");
        responseHeadersFilterList.remove("Set-Cookie");
        responseHeadersFilterList.remove("Trailer");
        responseHeadersFilterList.remove("Transfer-Encoding");
    }

    protected boolean isForwardedRequestHeader(String headerName) {
        return requestHeadersFilterList.contains(headerName);
    }

    protected boolean isForwardedResponseHeader(String headerName) {
        return responseHeadersFilterList.contains(headerName);
    }

    /**
     * Copy header from originalRequest to httpRequest.
     * <p>
     * Referer is rewritten. X-Forwarded-* headers are updated.
     * 
     * @param originalRequest
     *            source request
     * @param httpRequest
     *            destination request
     */
    public void copyHeaders(DriverRequest originalRequest, HttpRequest httpRequest) {
        String baseUrl = originalRequest.getBaseUrl().toString();
        String visibleBaseUrl = originalRequest.getVisibleBaseUrl();
        for (Header header : originalRequest.getOriginalRequest().getAllHeaders()) {
            String name = header.getName();
            // Special headers
            if (HttpHeaders.REFERER.equalsIgnoreCase(name) && isForwardedRequestHeader(HttpHeaders.REFERER)) {
                String value = header.getValue();
                value = urlRewriter.rewriteReferer(value, baseUrl, visibleBaseUrl);
                httpRequest.addHeader(name, value);
                // All other headers are copied if allowed
            } else if (isForwardedRequestHeader(name)) {
                httpRequest.addHeader(header);
            }
        }
        // process X-Forwarded-For header
        String remoteAddr = originalRequest.getOriginalRequest().getRemoteAddr();

        if (remoteAddr != null) {
            String forwardedFor = null;
            if (httpRequest.containsHeader("X-Forwarded-For")) {
                forwardedFor = httpRequest.getFirstHeader("X-Forwarded-For").getValue();
            }

            if (forwardedFor == null) {
                forwardedFor = remoteAddr;
            } else {
                forwardedFor = forwardedFor + ", " + remoteAddr;
            }

            httpRequest.setHeader("X-Forwarded-For", forwardedFor);
        }

        // Process X-Forwarded-Proto header
        if (!httpRequest.containsHeader("X-Forwarded-Proto")) {
            httpRequest.addHeader("X-Forwarded-Proto",
                    UriUtils.extractScheme(originalRequest.getOriginalRequest().getRequestLine().getUri()));
        }
    }

    /**
     * Copies end-to-end headers from a response received from the server to the response to be sent to the client.
     * 
     * @param outgoingRequest
     *            the request sent
     * @param incomingRequest
     *            the original request received from the client
     * @param httpClientResponse
     *            the response received from the provider application
     * @return the response to be sent to the client
     */
    public CloseableHttpResponse copyHeaders(OutgoingRequest outgoingRequest,
            HttpEntityEnclosingRequest incomingRequest, HttpResponse httpClientResponse) {
        HttpResponse result = new BasicHttpResponse(httpClientResponse.getStatusLine());
        result.setEntity(httpClientResponse.getEntity());
        String originalUri = incomingRequest.getRequestLine().getUri();
        String baseUrl = outgoingRequest.getBaseUrl().toString();
        String visibleBaseUrl = outgoingRequest.getOriginalRequest().getVisibleBaseUrl();
        for (Header header : httpClientResponse.getAllHeaders()) {
            String name = header.getName();
            String value = header.getValue();
            try {
                // Ignore Content-Encoding and Content-Type as these headers are
                // set in HttpEntity
                if (!HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(name)) {
                    if (isForwardedResponseHeader(name)) {
                        // Some headers containing an URI have to be rewritten
                        if (HttpHeaders.LOCATION.equalsIgnoreCase(name)
                                || HttpHeaders.CONTENT_LOCATION.equalsIgnoreCase(name)) {
                            // Header contains only an url
                            value = urlRewriter.rewriteUrl(value, originalUri, baseUrl, visibleBaseUrl, true);
                            value = HttpResponseUtils.removeSessionId(value, httpClientResponse);
                            result.addHeader(name, value);
                        } else if ("Link".equalsIgnoreCase(name)) {
                            // Header has the following format
                            // Link: </feed>; rel="alternate"

                            if (value.startsWith("<") && value.contains(">")) {
                                String urlValue = value.substring(1, value.indexOf(">"));

                                String targetUrlValue =
                                        urlRewriter.rewriteUrl(urlValue, originalUri, baseUrl, visibleBaseUrl, true);
                                targetUrlValue = HttpResponseUtils.removeSessionId(targetUrlValue, httpClientResponse);

                                value = value.replace("<" + urlValue + ">", "<" + targetUrlValue + ">");
                            }

                            result.addHeader(name, value);

                        } else if ("Refresh".equalsIgnoreCase(name)) {
                            // Header has the following format
                            // Refresh: 5; url=http://www.example.com
                            int urlPosition = value.indexOf("url=");
                            if (urlPosition >= 0) {
                                value = urlRewriter.rewriteRefresh(value, originalUri, baseUrl, visibleBaseUrl);
                                value = HttpResponseUtils.removeSessionId(value, httpClientResponse);
                            }
                            result.addHeader(name, value);
                        } else if ("P3p".equalsIgnoreCase(name)) {
                            // Do not translate url yet.
                            // P3P is used with a default fixed url most of the
                            // time.
                            result.addHeader(name, value);
                        } else {
                            result.addHeader(header.getName(), header.getValue());
                        }
                    }
                }
            } catch (Exception e1) {
                // It's important not to fail here.
                // An application can always send corrupted headers, and we
                // should not crash
                LOG.error("Error while copying headers", e1);
                result.addHeader("X-Esigate-Error", "Error processing header " + name + ": " + value);
            }
        }
        return BasicCloseableHttpResponse.adapt(result);
    }
}
