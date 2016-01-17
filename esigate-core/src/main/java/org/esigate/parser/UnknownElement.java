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

import org.esigate.HttpErrorPage;

/**
 * Handle unknown tag.
 * 
 * @author Alexis Thaveau
 */
public class UnknownElement implements Element {
    @Override
    public boolean onTagStart(String tag, ParserContext ctx) throws IOException, HttpErrorPage {
        // Write content in parent element
        ctx.characters(tag);
        return true;
    }

    @Override
    public void onTagEnd(String tag, ParserContext ctx) throws IOException, HttpErrorPage {

    }

    @Override
    public boolean onError(Exception e, ParserContext ctx) {
        return false;
    }

    @Override
    public void characters(CharSequence csq, int start, int end) throws IOException {
        throw new UnsupportedOperationException("characters are appended in onTagStart method");
    }

}
