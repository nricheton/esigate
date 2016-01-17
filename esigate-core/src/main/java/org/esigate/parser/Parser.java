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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.esigate.HttpErrorPage;
import org.esigate.impl.DriverRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parser {
    private static final Logger LOG = LoggerFactory.getLogger(Parser.class);
    private final Pattern pattern;
    private final List<ElementType> elementTypes;
    private DriverRequest httpRequest;
    private HttpResponse httpResponse;

    /**
     * Creates a Parser with a given regular expression pattern and ElementTypes.
     * 
     * @param pattern
     *            The regular expression Pattern
     * @param elementTypes
     *            The element types
     */
    public Parser(Pattern pattern, ElementType... elementTypes) {
        this.pattern = pattern;
        this.elementTypes = new ArrayList<ElementType>(elementTypes.length + 1);
        for (ElementType elementType : elementTypes) {
            this.elementTypes.add(elementType);
        }
        this.elementTypes.add(new UnknownElementType());

    }

    /**
     * Parses all the CharSequence.
     * 
     * @param in
     *            The CharSequence to parse
     * @param out
     *            The Writable to write the result to
     * @throws IOException
     * @throws HttpErrorPage
     */
    public void parse(CharSequence in, Appendable out) throws IOException, HttpErrorPage {
        ParserContextImpl ctx = new ParserContextImpl(out, httpRequest, httpResponse);
        Matcher matcher = pattern.matcher(in);
        int currentPosition = 0;
        while (matcher.find()) {
            String tag = matcher.group();
            ctx.characters(in, currentPosition, matcher.start());
            currentPosition = matcher.end();
            if (ctx.isCurrentTagEnd(tag)) {
                // check if this is the end tag for current element
                LOG.info("Processing end tag {}", tag);
                ctx.endElement(tag);
            } else {
                // if not, it is an opening tag for a new element
                LOG.info("Processing start tag {}", tag);
                ElementType type = null;
                for (ElementType t : elementTypes) {
                    if (t.isStartTag(tag)) {
                        type = t;
                        break;
                    }
                }
                Element element = type.newInstance();
                ctx.startElement(type, element, tag);
                if (type.isSelfClosing(tag)) {
                    ctx.endElement(tag);
                }

            }
        }
        // we reached the end of input
        ctx.characters(in, currentPosition, in.length());
    }

    public void setHttpRequest(DriverRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

}
