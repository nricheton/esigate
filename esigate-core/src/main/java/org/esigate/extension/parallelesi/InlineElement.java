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

package org.esigate.extension.parallelesi;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.esigate.parser.future.FutureElementType;
import org.esigate.parser.future.FutureParserContext;
import org.esigate.parser.future.StringBuilderFutureAppendable;
import org.esigate.util.UriUtils;

class InlineElement extends BaseElement {

    public static final FutureElementType TYPE = new BaseElementType("<esi:inline", "</esi:inline") {
        @Override
        public InlineElement newInstance() {
            return new InlineElement();
        }

    };

    private String uri;
    private boolean fetchable;
    private StringBuilderFutureAppendable buf = new StringBuilderFutureAppendable();

    InlineElement() {
    }

    @Override
    protected boolean parseTag(Tag tag, FutureParserContext ctx) {
        this.uri = tag.getAttribute("name");
        this.fetchable = "yes".equalsIgnoreCase(tag.getAttribute("fetchable"));
        return true;
    }

    @Override
    public void characters(Future<CharSequence> csq) {
        buf.enqueueAppend(csq);
    }

    @Override
    public void onTagEnd(String tag, FutureParserContext ctx) throws IOException {
        String originalUrl = UriUtils.getPath(ctx.getHttpRequest().getOriginalRequest().getRequestLine().getUri());
        try {
            InlineCache.storeFragment(uri, null, fetchable, originalUrl, buf.get().toString());
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }
}
