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

import java.text.SimpleDateFormat;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.esigate.servlet.HttpServletMediator;
import org.mockito.Mockito;

public class HttpServletMediatorTest extends TestCase {
	private SimpleDateFormat format;

	
	/**
	 * Ensure there is no exception when trying to create a session outside of a
	 * request (during background revalidation). Expected behavior is no
	 * exception, but value not set.
	 * 
	 * @see https://sourceforge.net/apps/mantisbt/webassembletool/view.php?id=229
	 * @throws Exception
	 */
	public void testSetAttributeNoSession() throws Exception {
		HttpServletRequest request = new MockHttpServletRequestBuilder().protocolVersion("HTTP/1.0").method("GET")
				.session(null).build();
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		ServletContext context = Mockito.mock(ServletContext.class);

		HttpServletMediator mediator = new HttpServletMediator(request, response, context);

		mediator.setSessionAttribute("test", "value");

		// Previous method should have no effect since session cannot be
		// created.
		Assert.assertNull(mediator.getSessionAttribute("test"));
	}

}
