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

package org.esigate.impl;

import java.net.URI;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.esigate.util.UriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "fixes" links to resources, images and pages in pages retrieved by esigate :
 * <ul>
 * <li>Current-path-relative urls are converted to full path relative urls ( img/test.img ->
 * /myapp/curentpath/img/test.img)</li>
 * <li>All relative urls can be converted to absolute urls (including server name)</li>
 * </ul>
 * 
 * This enables use of esigate without any special modifications of the generated urls on the provider side.
 * 
 * All href and src attributes are processed, except javascript links.
 * 
 * @author Nicolas Richeton
 * 
 */
public class UrlRewriter {
    private static final Logger LOG = LoggerFactory.getLogger(UrlRewriter.class);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "<([^\\!:>]+)(src|href|action|background|content)\\s*=\\s*('[^<']*'|\"[^<\"]*\")([^>]*)>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern JAVASCRIPT_CONCATENATION_PATTERN = Pattern.compile(
            "\\+\\s*'|\\+\\s*\"|'\\s*\\+|\"\\s*\\+", Pattern.CASE_INSENSITIVE);

    private static final Pattern META_REFRESH_PATTERN = Pattern.compile(
            "<\\s*meta([^>]+)http-equiv\\s*=\\s*(\"|')refresh(\"|')", Pattern.CASE_INSENSITIVE);

    /**
     * Rewrites urls from the response for the client or from the request to the target server.
     * 
     * If mode is ABSOLUTE, all relative urls will be replaced by the full urls :
     * <ul>
     * <li>images/image.png is replaced by http://server/context/images/image.png</li>
     * <li>/context/images/image.png is replaced by http://server/context/images/image.png</li>
     * </ul>
     * 
     * If mode is RELATIVE, context will be added to relative urls :
     * <ul>
     * <li>images/image.png is replaced by /context/images/image.png</li>
     * </ul>
     * 
     * @param properties
     *            Configuration properties
     * 
     */
    public UrlRewriter(Properties properties) {
    }

    /**
     * Fixes a referer url in a request.
     * 
     * @param referer
     *            the url to fix (can be anything found in an html page, relative, absolute, empty...)
     * @param baseUrl
     *            The base URL selected for this request.
     * @param visibleBaseUrl
     *            The base URL viewed by the browser.
     * 
     * @return the fixed url.
     */
    public String rewriteReferer(String referer, String baseUrl, String visibleBaseUrl) {
        URI uri = UriUtils.createURI(referer);

        // Base url should end with /
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        URI baseUri = UriUtils.createURI(baseUrl);

        // If no visible url base is defined, use base url as visible base url
        if (!visibleBaseUrl.endsWith("/")) {
            visibleBaseUrl = visibleBaseUrl + "/";
        }
        URI visibleBaseUri = UriUtils.createURI(visibleBaseUrl);

        // Relativize url to visible base url
        URI relativeUri = visibleBaseUri.relativize(uri);
        // If the url is unchanged do nothing
        if (relativeUri.equals(uri)) {
            LOG.debug("url kept unchanged: [{}]", referer);
            return referer;
        }
        // Else rewrite replacing baseUrl by visibleBaseUrl
        URI result = baseUri.resolve(relativeUri);
        LOG.debug("referer fixed: [{}] -> [{}]", referer, result);
        return result.toString();
    }

    /**
     * Fixes an url according to the chosen mode.
     * 
     * @param url
     *            the url to fix (can be anything found in an html page, relative, absolute, empty...)
     * @param requestUrl
     *            The incoming request URL (could be absolute or relative to visible base url).
     * @param baseUrl
     *            The base URL selected for this request.
     * @param visibleBaseUrl
     *            The base URL viewed by the browser.
     * @param absolute
     *            Should the rewritten urls contain the scheme host and port
     * 
     * @return the fixed url.
     */
    public String rewriteUrl(String url, String requestUrl, String baseUrl, String visibleBaseUrl, boolean absolute) {
        // Base url should end with /
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        URI baseUri = UriUtils.createURI(baseUrl);

        // If no visible url base is defined, use base url as visible base url
        if (!visibleBaseUrl.endsWith("/")) {
            visibleBaseUrl = visibleBaseUrl + "/";
        }
        URI visibleBaseUri = UriUtils.createURI(visibleBaseUrl);

        // Build the absolute Uri of the request sent to the backend
        URI requestUri;
        if (requestUrl.startsWith(visibleBaseUrl)) {
            requestUri = UriUtils.createURI(requestUrl);
        } else {
            requestUri = UriUtils.concatPath(baseUri, requestUrl);
        }

        // Interpret the url relatively to the request url (may be relative)
        URI uri = UriUtils.resolve(url, requestUri);
        // Normalize the path (remove . or .. if possible)
        uri = uri.normalize();

        // Try to relativize url to base url
        URI relativeUri = baseUri.relativize(uri);
        // If the url is unchanged do nothing
        if (relativeUri.equals(uri)) {
            LOG.debug("url kept unchanged: [{}]", url);
            return url;
        }
        // Else rewrite replacing baseUrl by visibleBaseUrl
        URI result = visibleBaseUri.resolve(relativeUri);
        // If mode relative, remove all the scheme://host:port to keep only a url relative to server root (starts with
        // "/")
        if (!absolute) {
            result = UriUtils.removeServer(result);
        }
        LOG.debug("url fixed: [{}] -> [{}]", url, result);
        return result.toString();
    }

    /**
     * Fixes all resources urls and returns the result.
     * 
     * @param input
     *            The html to be processed.
     * 
     * @param requestUrl
     *            The request URL.
     * @param baseUrlParam
     *            The base URL selected for this request.
     * @param visibleBaseUrl
     *            The base URL viewed by the browser.
     * @param absolute
     *            Should the rewritten urls contain the scheme host and port
     * 
     * @return the result of this renderer.
     */
    public CharSequence rewriteHtml(CharSequence input, String requestUrl, String baseUrlParam, String visibleBaseUrl,
            boolean absolute) {
        StringBuffer result = new StringBuffer(input.length());
        Matcher m = URL_PATTERN.matcher(input);
        while (m.find()) {
            String url = input.subSequence(m.start(3) + 1, m.end(3) - 1).toString();
            String tag = m.group(0);
            String quote = input.subSequence(m.end(3) - 1, m.end(3)).toString();

            // Browsers tolerate urls with white spaces before or after
            String trimmedUrl = StringUtils.trim(url);

            String rewrittenUrl = url;

            trimmedUrl = unescapeHtml(trimmedUrl);

            if (trimmedUrl.isEmpty()) {
                LOG.debug("empty url kept unchanged");
            } else if (trimmedUrl.startsWith("#")) {
                LOG.debug("anchor url kept unchanged: [{}]", url);
            } else if (JAVASCRIPT_CONCATENATION_PATTERN.matcher(trimmedUrl).find()) {
                LOG.debug("url in javascript kept unchanged: [{}]", url);
            } else if (m.group(2).equalsIgnoreCase("content")) {
                if (META_REFRESH_PATTERN.matcher(tag).find()) {
                    rewrittenUrl = rewriteRefresh(trimmedUrl, requestUrl, baseUrlParam, visibleBaseUrl);
                    rewrittenUrl = escapeHtml(rewrittenUrl);
                    LOG.debug("refresh url [{}] rewritten [{}]", url, rewrittenUrl);
                } else {
                    LOG.debug("content attribute kept unchanged: [{}]", url);
                }
            } else {
                rewrittenUrl = rewriteUrl(trimmedUrl, requestUrl, baseUrlParam, visibleBaseUrl, absolute);
                rewrittenUrl = escapeHtml(rewrittenUrl);
                LOG.debug("url [{}] rewritten [{}]", url, rewrittenUrl);
            }

            m.appendReplacement(result, ""); // Copy what is between the previous match and the current match
            result.append("<");
            result.append(m.group(1));
            result.append(m.group(2));
            result.append("=");
            result.append(quote);
            result.append(rewrittenUrl);
            result.append(quote);
            if (m.groupCount() > 3) {
                result.append(m.group(4));
            }
            result.append(">");
        }

        m.appendTail(result); // Copy the reminder of the input

        return result;
    }

    private String unescapeHtml(String url) {
        // Unescape entities, ex: &apos; or &#39;
        url = StringEscapeUtils.unescapeHtml4(url);
        return url;
    }

    private String escapeHtml(String url) {
        // Escape the previously unescaped characters
        url = StringEscapeUtils.escapeHtml4(url);
        // Replace " by &quot; in order not to break the html
        url = url.replaceAll("'", "&apos;");
        url = url.replaceAll("\"", "&quot;");
        return url;
    }

    /**
     * Rewrites a "Refresh" HTTP header or a &lt;meta http-equiv="refresh"... tag. The value should have the following
     * format:
     * 
     * Refresh: 5; url=http://www.example.com
     * 
     * @param input
     *            The refresh value to be rewritten.
     * @param requestUrl
     *            The request URL.
     * @param baseUrl
     *            The base URL selected for this request.
     * @param visibleBaseUrl
     *            The base URL viewed by the browser.
     * @return the rewritten refresh value
     */
    public String rewriteRefresh(String input, String requestUrl, String baseUrl, String visibleBaseUrl) {
        // Header has the following format
        // Refresh: 5; url=http://www.w3.org/pub/WWW/People.html
        int urlPosition = input.indexOf("url=");
        if (urlPosition >= 0) {
            String urlValue = input.substring(urlPosition + "url=".length());
            String targetUrlValue = rewriteUrl(urlValue, requestUrl, baseUrl, visibleBaseUrl, true);
            return input.substring(0, urlPosition) + "url=" + targetUrlValue;
        } else {
            return input;
        }
    }
}
