package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * a filter reader implementation to filter out HTML tags from the base reader
 */
public class TagFilteringReader extends FilterReader {

    public static final String[] DEFAULT_TO_RENAME = new String[]{"html:div class=\"proxy-html-content\""};
    public static final String[] DEFAULT_TO_STRIP = new String[]{"body"};
    public static final String[] DEFAULT_TO_DROP = new String[]{"head", "style", "script", "link"};

    private enum Debug {none, read, strip, drop} // switch debug on for unit test debugging

    private Debug debug = Debug.none;

    /**
     * the strategy to handle tokens of an 'open' tag
     */
    interface TagFilter {

        String tagName();

        /**
         * the tag start handler with the token which has triggered this handler
         */
        int start(int token) throws IOException;

        /**
         * the tag body handler
         */
        int body() throws IOException;

        /**
         * the tag end handler
         */
        int end() throws IOException;

        void debug(Character hint);
    }

    /**
     * the abstract tag handler base implementation
     */
    protected abstract class TagFilterBase implements TagFilter {

        protected final String tagName;

        protected TagFilterBase(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public String tagName() {
            return tagName;
        }

        /**
         * scan and discard all tokens of the tag start up to the tags body
         *
         * @param token the first token after the tag name
         * @return the token after the tags start
         * - the first token of the body or
         * - the first token of the tag end if the body is empty or
         * - the token after the tag if the tag start is closing the tag imeadiately
         */
        @Override
        public int start(int token) throws IOException {
            Integer last = null;
            while (token != '>') {
                last = token;
                token = scan();
            }
            if (last != null && last == '/') {
                // tag ends at the end of start; no body
                openTags.pop();
                debug('#');
            } else {
                debug(null);
            }
            return read();
        }

        /**
         * @return return the next non space token after the closed tag
         */
        @Override
        public int end() throws IOException {
            debug('/');
            return read();
        }
    }

    /**
     * the tag handler implementation for tags to 'stripe'
     */
    protected class RenameTagFilter extends StripTagFilter {

        protected final String newTagName;
        protected final String attributes;

        protected RenameTagFilter(String tagName, String newTagName) {
            super(tagName);
            this.newTagName = StringUtils.substringBefore(newTagName, " ");
            this.attributes = StringUtils.substringAfter(newTagName, " ");
        }

        /**
         * @return the buffer() for the tag start with new new tag name
         */
        @Override
        public int start(int token) {
            StringBuilder tagName = new StringBuilder(newTagName);
            if (StringUtils.isNotBlank(attributes)) {
                tagName.append(" ").append(attributes);
            }
            tagName.append((char) token);
            return buffer(tagName);
        }

        /**
         * @return the buffer() for the end of the tag with the new tag name
         */
        @Override
        public int end() {
            StringBuilder tagName = new StringBuilder("/");
            tagName.append(newTagName).append('>');
            debug('/');
            return buffer(tagName);
        }

        @Override
        public void debug(Character hint) {
            if (debug == Debug.strip) {
                System.out.println("<" + (hint != null ? hint : "") + tagName + ":" + newTagName + ">");
            }
        }
    }

    /**
     * the tag handler implementation for tags to 'stripe'
     */
    protected class StripTagFilter extends TagFilterBase {

        protected StripTagFilter(String tagName) {
            super(tagName);
        }

        /**
         * @return each useful token of the tags body (uses 'scan' to filter out tags inside of the body)
         */
        @Override
        public int body() throws IOException {
            int token = scan();
            if (debug == Debug.strip) {
                System.out.println((char) token);
            }
            return token;
        }

        @Override
        public void debug(Character hint) {
            if (debug == Debug.strip) {
                System.out.println("<" + (hint != null ? hint : "") + tagName + ">");
            }
        }
    }

    /**
     * the tag handler implementation for tags to 'drop'
     */
    protected class DropTagFilter extends TagFilterBase {

        protected DropTagFilter(String tagName) {
            super(tagName);
        }

        /**
         * @return the token after discarding all tokens of the handlers tag and the tags body
         */
        @Override
        public int body() throws IOException {
            int token;
            while ((token = scan()) >= 0 && !openTags.isEmpty() && openTags.peek() == this) {
                if (debug == Debug.drop) {
                    System.err.println((char) token);
                }
            }
            return token;
        }

        @Override
        public void debug(Character hint) {
            if (debug == Debug.drop) {
                System.out.println("[" + (hint != null ? hint : "") + tagName + "]");
            }
        }
    }

    /**
     * the tag names configuration of the filter reader
     */
    protected final Map<String, String> toRename;
    protected final List<String> toStrip;
    protected final List<String> toDrop;

    /**
     * the stack of handlers for open tags - the tomost handler is used for token reading
     */
    protected Stack<TagFilter> openTags = new Stack<>();

    /**
     * the buffer filled if a 'look forward' to find a tag name has found no tag to filter out
     */
    protected char[] buffer = null;
    protected int bufferPos;

    /**
     * Creates a tag filtering reader to filter out the tags configured as sets of tag names
     *
     * @param in       the reader to filter during read
     * @param toRename the set of tag names which should be kept but renamed (e.g. 'html:div' to keep the root)
     * @param toStrip  the set of tag names to 'stripe' - remove the tags around and keep the body of the tags
     * @param toDrop   the set of tag names to 'drop' - remove the tags including their body
     */
    public TagFilteringReader(@NotNull final Reader in, @NotNull final String[] toRename,
                              @NotNull final String[] toStrip, @NotNull final String[] toDrop) {
        super(in);
        this.toRename = new HashMap<>();
        for (String rule : toRename) {
            if (StringUtils.isNotBlank(rule)) {
                String[] split = StringUtils.split(rule, ":", 2);
                this.toRename.put(split[0], split[1]);
            }
        }
        this.toStrip = Arrays.asList(toStrip);
        this.toDrop = Arrays.asList(toDrop);
    }

    public TagFilteringReader(@NotNull final InputStream in, @NotNull final String[] toRename,
                              @NotNull final String[] toStrip, @NotNull String[] toDrop) {
        this(new InputStreamReader(in, StandardCharsets.UTF_8), toRename, toStrip, toDrop);
    }

    public TagFilteringReader(@NotNull final InputStream in) {
        this(in, DEFAULT_TO_RENAME, DEFAULT_TO_STRIP, DEFAULT_TO_DROP);
    }

    public TagFilteringReader(@NotNull final Reader in) {
        this(in, DEFAULT_TO_RENAME, DEFAULT_TO_STRIP, DEFAULT_TO_DROP);
    }

    //
    // Reader...
    //

    /**
     * uses the single token read() method to fill the buffer
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int count;
        for (count = 0; count < len; count++) {
            int token = read();
            if (token < 0) {
                return count > 0 ? count : -1;
            }
            cbuf[off + count] = (char) token;
        }
        return count;
    }

    /**
     * delegates the read to the current tag handler or to the 'scan' if no such handler is active
     * (the handlers are also using the 'scan' method to find tags inside of open tags)
     */
    @Override
    public int read() throws IOException {
        int token;
        if (openTags.isEmpty()) {
            token = scan();
        } else {
            token = openTags.peek().body();
        }
        return token;
    }

    //
    // Filter...
    //

    /**
     * scans the token from the 'original' reader and determines tag starts / ends and the tag names
     * controls the tag handler stack if tags found which should be filtered
     *
     * @return the next token for the readers 'read' method
     */
    protected int scan() throws IOException {
        int token;
        if (buffer != null) { // a filled buffer has precendence, a scan is always done in this case
            token = buffer[bufferPos];
            bufferPos++;
            if (bufferPos >= buffer.length) {
                buffer = null;
            }
        } else {
            token = next();
            if (token >= 0) {
                if (token == '<') {
                    if ((token = next()) >= 0) {
                        StringBuilder tagName = new StringBuilder();
                        if (token == '/') {
                            // tag end...
                            while ((token = next()) >= 0) {
                                if (token == '>') {
                                    TagFilter filter;
                                    if (!openTags.isEmpty() &&
                                            (filter = openTags.peek()).tagName().equals(tagName.toString().toLowerCase())) {
                                        // this tag end closes the topmost tag of the tag handlers stack...
                                        openTags.pop();
                                        return filter.end();
                                    } else if (openTags.size() > 1 && (filter = openTags.get(openTags.size() - 2))
                                            .tagName().equals(tagName.toString().toLowerCase())) {
                                        // assuming that the topmost tag is not closed (not well formed)...
                                        openTags.pop();
                                        openTags.pop();
                                        return filter.end();
                                    } else {
                                        // the tag end is not an end of a configured tag
                                        // fill the buffer to flush this tag end
                                        tagName.insert(0, '/');
                                        tagName.append((char) token);
                                        return buffer(tagName);
                                    }
                                } else {
                                    // collect the tokens to build the tag name
                                    tagName.append((char) token);
                                }
                            }
                            // unexpected EOF - flush the last buffered tokens...
                            tagName.insert(0, '/');
                        } else {
                            // tag start...
                            do {
                                if (token == ' ' || token == '>' || token == '/') {
                                    // tag name delimiter reached...
                                    TagFilter filter = getFilter(tagName.toString().toLowerCase());
                                    if (filter != null) {
                                        // this tag has to be filtered - use the filter as the current handler
                                        openTags.push(filter);
                                        return filter.start(token);
                                    } else {
                                        // the tag start is not a start of a configured tag
                                        // fill the buffer to flush this tag start
                                        tagName.append((char) token);
                                        return buffer(tagName);
                                    }
                                } else {
                                    // collect the tokens to build the tag name
                                    tagName.append((char) token);
                                }
                            } while ((token = next()) >= 0);
                        }
                        // unexpected EOF - flush the last buffered tokens...
                        return buffer(tagName);
                    } else {
                        // unexpected EOF - return the last '<'
                        token = '<';
                    }
                }
            }
        }
        return token;
    }

    /**
     * @return the next token from the 'original' reader
     */
    protected int next() throws IOException {
        int token = in.read();
        if (debug == Debug.read) {
            System.out.print((char) token);
        }
        return token;
    }

    /**
     * @return the next non space token
     */
    protected int trim() throws IOException {
        int token;
        while (Character.isWhitespace(token = read())) ; // next non space token...
        return token;
    }

    /**
     * fills up the buffer with the string builders content and returns a 'tag start' tokone ('<')
     */
    protected int buffer(StringBuilder tagName) {
        buffer = tagName.toString().toCharArray();
        bufferPos = 0;
        return '<';
    }

    /**
     * @return the filter to handle the found tag name; 'null' if the tag name is not configured for this filter
     */
    protected TagFilter getFilter(String tagName) {
        TagFilter current = openTags.isEmpty() ? null : openTags.peek();
        if (current instanceof DropTagFilter) {
            return new DropTagFilter(tagName);
        } else {
            if (toRename.containsKey(tagName)) {
                return new RenameTagFilter(tagName, toRename.get(tagName));
            } else if (toStrip.contains(tagName)) {
                return new StripTagFilter(tagName);
            } else if (toDrop.contains(tagName)) {
                return new DropTagFilter(tagName);
            }
        }
        return null;
    }
}
