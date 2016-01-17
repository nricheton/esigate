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

package org.esigate.parser;

import java.io.IOException;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.commons.io.output.StringBuilderWriter;
import org.esigate.HttpErrorPage;
import org.esigate.MockRequestExecutor;
import org.esigate.impl.DriverRequest;
import org.esigate.test.TestUtils;
import org.esigate.util.HttpRequestHelper;

public class ParserTest extends TestCase {
    private Parser tested;

    @Override
    protected void setUp() throws HttpErrorPage {
        MockRequestExecutor provider = MockRequestExecutor.createMockDriver();
        tested = new Parser(Pattern.compile("(<test:[^>]*>)|(</test:[^>]*>)"), SIMPLE, BODY);
        DriverRequest request = TestUtils.createDriverRequest("http://a.b?request=updated", provider.getDriver());
        tested.setHttpRequest(request);
    }

    @Override
    protected void tearDown() {
        tested = null;
    }

    public void testParse() throws IOException, HttpErrorPage {
        String page =
                "begin " + "<test:simple name='ignored'> this text will be ignored </test:simple>"
                        + "<test:body>this text should be {request} </test:body>" + "<test:unknown name='value' />"
                        + "<test:simple name='also ignored'/>" + " end";
        StringBuilderWriter out = new StringBuilderWriter();

        tested.parse(page, out);
        assertEquals("begin this text should be updated <test:unknown name='value' /> end", out.toString());
    }

    private static final ElementType SIMPLE = new MockElementType("<test:simple", "</test:simple") {
        @Override
        public Element newInstance() {
            return new SimpleElement();
        }
    };
    private static final ElementType BODY = new MockElementType("<test:body", "</test:body") {
        @Override
        public Element newInstance() {
            return new BodyElement();
        }
    };

    private abstract static class MockElementType implements ElementType {
        private final String startTag;
        private final String endTag;

        protected MockElementType(String startTag, String endTag) {
            this.startTag = startTag;
            this.endTag = endTag;
        }

        @Override
        public final boolean isStartTag(String tag) {
            return tag.startsWith(startTag);
        }

        @Override
        public final boolean isEndTag(String tag) {
            return tag.startsWith(endTag);
        }

        @Override
        public boolean isSelfClosing(String tag) {
            return tag.endsWith("/>");
        }
    }

    private static class BodyElement extends SimpleElement {
        private final StringBuilder buf = new StringBuilder();

        public BodyElement() {
        }

        @Override
        public void onTagEnd(String tag, ParserContext ctx) throws IOException {
            String result =
                    buf.toString().replaceAll("\\{request\\}",
                            HttpRequestHelper.getParameter(ctx.getHttpRequest(), "request"));
            ctx.getCurrent().characters(result, 0, result.length());
        }

        @Override
        public void characters(CharSequence csq, int start, int end) {
            buf.append(csq, start, end);
        }
    }

    private static class SimpleElement implements Element {

        public SimpleElement() {
        }

        @Override
        public boolean onTagStart(String tag, ParserContext ctx) {
            return true;
        }

        @Override
        public void onTagEnd(String tag, ParserContext ctx) throws IOException {
        }

        @Override
        public void characters(CharSequence csq, int start, int end) {
        }

        @Override
        public boolean onError(Exception e, ParserContext ctx) {
            return false;
        }

    }
}
