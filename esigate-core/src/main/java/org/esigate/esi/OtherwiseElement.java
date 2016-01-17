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

import org.esigate.Parameters;
import org.esigate.parser.ElementType;
import org.esigate.parser.ParserContext;

class OtherwiseElement extends BaseElement {

    public static final ElementType TYPE = new BaseElementType("<esi:otherwise", "</esi:otherwise") {
        @Override
        public OtherwiseElement newInstance() {
            return new OtherwiseElement();
        }

    };

    private boolean active;
    private StringBuilder buf = new StringBuilder(Parameters.DEFAULT_BUFFER_SIZE);

    OtherwiseElement() {
    }

    @Override
    protected boolean parseTag(Tag tag, ParserContext ctx) {
        ChooseElement parent = ctx.findAncestor(ChooseElement.class);
        active = (parent != null) && !parent.hadConditionSet();
        return active;
    }

    @Override
    public void onTagEnd(String tag, ParserContext ctx) throws IOException {
        if (active) {
            super.characters(buf, 0, buf.length());
        }
    }

    @Override
    public void characters(CharSequence csq, int start, int end) {
        if (active) {
            buf.append(csq, start, end);
        }
    }
}
