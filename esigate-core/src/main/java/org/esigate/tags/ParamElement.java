package org.esigate.tags;

import java.io.IOException;

import org.esigate.aggregator.AggregationSyntaxException;
import org.esigate.parser.Element;
import org.esigate.parser.ElementType;
import org.esigate.parser.ParserContext;

class ParamElement implements Element {
    public static final ElementType TYPE = new ElementType() {

        @Override
        public boolean isStartTag(String tag) {
            return tag.startsWith("<!--$beginparam$");
        }

        @Override
        public boolean isEndTag(String tag) {
            return tag.startsWith("<!--$endparam$");
        }

        @Override
        public Element newInstance() {
            return new ParamElement();
        }

        @Override
        public boolean isSelfClosing(String tag) {
            return false;
        }

    };

    private Element parent;
    private boolean valueFound = false;

    @Override
    public boolean onError(Exception e, ParserContext ctx) {
        return false;
    }

    @Override
    public void onTagEnd(String tag, ParserContext ctx) {
        // Nothing to do
    }

    @Override
    public boolean onTagStart(String tag, ParserContext ctx) throws IOException {
        this.parent = ctx.getCurrent();
        String[] parameters = tag.split("\\$");
        if (parameters.length != 4) {
            throw new AggregationSyntaxException("Invalid syntax: " + tag);
        }
        String name = parameters[2];
        TemplateElement templateElement = ctx.findAncestor(TemplateElement.class);
        TemplateRenderer templateRenderer = ctx.findAncestor(TemplateRenderer.class);
        if (templateElement == null || templateElement.isNameMatches()) {
            String value = templateRenderer.getParam(name);
            if (value != null) {
                parent.characters(value, 0, value.length());
                valueFound = true;
            }
        }
        return true;
    }

    @Override
    public void characters(CharSequence csq, int start, int end) throws IOException {
        if (!valueFound) {
            parent.characters(csq, start, end);
        }
    }

}
