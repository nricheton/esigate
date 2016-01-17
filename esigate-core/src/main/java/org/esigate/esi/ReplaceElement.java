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

import org.esigate.HttpErrorPage;
import org.esigate.Parameters;
import org.esigate.parser.ElementType;
import org.esigate.parser.ParserContext;

/**
 * Support for &lt;esi:replace&gt; element inside of parent &lt;esi:include&gt;
 * 
 * @author <a href="stanislav.bernatskyi@smile.fr">Stanislav Bernatskyi</a>
 */
class ReplaceElement extends BaseElement {

    public static final ElementType TYPE = new BaseElementType("<esi:replace", "</esi:replace") {
        @Override
        public ReplaceElement newInstance() {
            return new ReplaceElement();
        }

    };

    private StringBuilder buf = null;
    private String fragment;
    private String regexp;

    @Override
    public void characters(CharSequence csq, int start, int end) {
        buf.append(csq, start, end);
    }

    @Override
    public void onTagEnd(String tag, ParserContext ctx) throws HttpErrorPage {
        IncludeElement parent = ctx.findAncestor(IncludeElement.class);
        if (parent == null) {
            throw new EsiSyntaxError("<esi:replace> tag can only be used inside an <esi:include> tag");
        }
        String result = buf.toString();
        if (fragment != null) {
            parent.addFragmentReplacement(fragment, result);
        } else if (regexp != null) {
            parent.addRegexpReplacement(regexp, result);
        } else {
            parent.characters(result, 0, result.length());
        }
    }

    @Override
    protected boolean parseTag(Tag tag, ParserContext ctx) throws HttpErrorPage {
        buf = new StringBuilder(Parameters.DEFAULT_BUFFER_SIZE);
        fragment = tag.getAttribute("fragment");
        regexp = tag.getAttribute("regexp");
        if (regexp == null) {
            regexp = tag.getAttribute("expression");
        }
        if ((fragment == null && regexp == null) || (fragment != null && regexp != null)) {
            throw new EsiSyntaxError("only one of 'fragment' and 'expression' attributes is allowed");
        }
        return true;
    }

}
