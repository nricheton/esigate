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

import org.esigate.Parameters;
import org.esigate.impl.DriverRequest;
import org.esigate.util.UriUtils;

/**
 * Utility class to generate URL and path for Resources.
 * 
 * @author Francois-Xavier Bonnet
 */
public final class ResourceUtils {

    /**
     * Private constructor
     */
    private ResourceUtils() {

    }

    private static String buildQueryString(DriverRequest originalRequest, boolean proxy) {
        StringBuilder queryString = new StringBuilder(Parameters.SMALL_BUFFER_SIZE);

        String originalQuerystring =
                UriUtils.getRawQuery(originalRequest.getOriginalRequest().getRequestLine().getUri());
        if (proxy && originalQuerystring != null) {
            // Remove jsessionid from request if it is present
            // As we are in a java application, the container might add
            // jsessionid to the querystring. We must not forward it to
            // included applications.
            String jsessionid = null;
            jsessionid = originalRequest.getOriginalRequest().getSessionId();
            if (jsessionid != null) {
                originalQuerystring = UriUtils.removeSessionId(jsessionid, originalQuerystring);
            }
            queryString.append(originalQuerystring);
        }
        return queryString.toString();
    }

    private static String concatUrl(String baseUrl, String relUrl) {
        StringBuilder url = new StringBuilder(Parameters.SMALL_BUFFER_SIZE);
        if (baseUrl != null && relUrl != null && (baseUrl.endsWith("/") || baseUrl.endsWith("\\"))
                && relUrl.startsWith("/")) {
            url.append(baseUrl.substring(0, baseUrl.length() - 1)).append(relUrl);
        } else {
            url.append(baseUrl).append(relUrl);
        }
        return url.toString();
    }

    public static String getHttpUrlWithQueryString(String url, DriverRequest originalRequest, boolean proxy) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Relative URL, we need to add the driver base url
            String baseUrl = originalRequest.getBaseUrl().toString();
            if (baseUrl != null) {
                url = concatUrl(baseUrl, url);
            }
        }
        String queryString = ResourceUtils.buildQueryString(originalRequest, proxy);
        if (queryString.length() == 0) {
            return url;
        } else {
            return url + "?" + queryString;
        }
    }

}
