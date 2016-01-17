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

import org.esigate.Parameters;
import org.esigate.parser.ElementType;
import org.esigate.parser.ParserContext;
import org.esigate.util.UriUtils;

class InlineElement extends BaseElement {

    public static final ElementType TYPE = new BaseElementType("<esi:inline", "</esi:inline") {
        @Override
        public InlineElement newInstance() {
            return new InlineElement();
        }

    };

    private String uri;
    private boolean fetchable;
    private StringBuilder buf = new StringBuilder(Parameters.DEFAULT_BUFFER_SIZE);

    InlineElement() {
    }

    @Override
    protected boolean parseTag(Tag tag, ParserContext ctx) {
        this.uri = tag.getAttribute("name");
        this.fetchable = "yes".equalsIgnoreCase(tag.getAttribute("fetchable"));
        return true;
    }

    @Override
    public void characters(CharSequence csq, int start, int end) {
        buf.append(csq, start, end);
    }

    @Override
    public void onTagEnd(String tag, ParserContext ctx) {
        String originalUrl = UriUtils.getPath(ctx.getHttpRequest().getOriginalRequest().getRequestLine().getUri());
        InlineCache.storeFragment(uri, null, fetchable, originalUrl, buf.toString());
    }
}
