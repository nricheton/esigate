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

package org.esigate.extension;

import java.util.Properties;

import org.apache.http.HttpStatus;
import org.esigate.Driver;
import org.esigate.Parameters;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.FetchEvent;
import org.esigate.events.impl.RenderEvent;
import org.esigate.http.IncomingRequest;
import org.esigate.test.TestUtils;
import org.esigate.test.conn.SequenceResponse;
import org.esigate.test.driver.AbstractDriverTestCase;
import org.junit.Assert;

/**
 * Tests on the API contract.
 * 
 * @author Nicolas Richeton
 * 
 */
public class APITest extends AbstractDriverTestCase {

    /**
     * Ensure all events are correctly initialized.
     * 
     * @throws Exception
     */
    public void testEventInit() throws Exception {

        // Conf
        Properties properties = new Properties();
        properties.put(Parameters.REMOTE_URL_BASE.getName(), "http://provider/");
        properties.put(Parameters.EXTENSIONS, AssertEventInit.class.getName());

        // Setup remote server (provider) response.
        Driver driver =
                TestUtils.createMockDriver(
                        properties,
                        new SequenceResponse().response(TestUtils.createHttpResponse().status(HttpStatus.SC_OK)
                                .reason("OK").header("Content-Type", "text/html; charset=utf-8").build()));

        IncomingRequest request = TestUtils.createRequest("http://test.mydomain.fr/foobar/").build();

        TestUtils.driverProxy(driver, request);

    }

    /**
     * Ensure all events are correctly initialized.
     * 
     */
    public static class AssertEventInit implements Extension {

        @Override
        public void init(Driver driver, Properties properties) {
            driver.getEventManager().register(EventManager.EVENT_RENDER_PRE, new IEventListener() {

                @Override
                public boolean event(EventDefinition id, Event event) {
                    RenderEvent revent = (RenderEvent) event;
                    Assert.assertNotNull("httpResponse should not be null", revent.getHttpResponse());
                    Assert.assertNotNull("originalRequest should not be null", revent.getOriginalRequest());
                    Assert.assertNotNull("renderers should not be null", revent.getRenderers());
                    return true;
                }
            });
            
            
            driver.getEventManager().register(EventManager.EVENT_FETCH_POST, new IEventListener() {

                @Override
                public boolean event(EventDefinition id, Event event) {
                    FetchEvent revent = (FetchEvent) event;
                    Assert.assertNotNull("httpResponse should not be null", revent.getHttpResponse());
                    Assert.assertNotNull("request should not be null", revent.getHttpRequest());
                    Assert.assertNotNull("context should not be null", revent.getHttpContext());
                    return true;
                }
            });

        }

    }
    
    
    
  
    
 
    
}
