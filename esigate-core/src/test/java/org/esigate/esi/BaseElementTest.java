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

package org.esigate.esi;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.http.HttpResponse;
import org.esigate.impl.DriverRequest;
import org.esigate.parser.Element;
import org.esigate.parser.ParserContext;

public class BaseElementTest extends TestCase {

    public void testOnTagStart() throws Exception {
        final Tag[] parsed = new Tag[1];
        BaseElement tested = new MockBaseElement() {
            @Override
            protected boolean parseTag(Tag tag, ParserContext ctx) {
                parsed[0] = tag;
                return true;
            }
        };
        ParserContext ctx = new MockParserContext();

        tested.onTagStart("<do:something />", ctx);

        assertNotNull(parsed[0]);
        assertEquals(true, parsed[0].isOpenClosed());
        assertEquals(false, parsed[0].isClosing());
        assertEquals("do:something", parsed[0].getName());

        tested.onTagStart("<do:something>", ctx);

        assertNotNull(parsed[0]);
        assertEquals(false, parsed[0].isOpenClosed());
        assertEquals(false, parsed[0].isClosing());
        assertEquals("do:something", parsed[0].getName());

        tested.onTagStart("<do:something name='value'>", ctx);

        assertNotNull(parsed[0]);
        assertEquals(false, parsed[0].isOpenClosed());
        assertEquals(false, parsed[0].isClosing());
        assertEquals("do:something", parsed[0].getName());
        assertEquals("value", parsed[0].getAttribute("name"));
    }

    protected static class MockBaseElement extends BaseElement {
        public MockBaseElement() {
        }

        @Override
        public void onTagEnd(String tag, ParserContext ctx) {
            // Nothing to do
        }

    }

    protected static class MockParserContext implements ParserContext {
        @Override
        public DriverRequest getHttpRequest() {
            return null;
        }

        @Override
        public boolean reportError(Exception e) {
            return false;
        }

        @Override
        public Element getCurrent() {
            return null;
        }

        @Override
        public <T> T findAncestor(Class<T> type) {
            return null;
        }

        @Override
        public HttpResponse getHttpResponse() {
            return null;
        }

        @Override
        public void characters(CharSequence cs) throws IOException {

        }

    }
}
