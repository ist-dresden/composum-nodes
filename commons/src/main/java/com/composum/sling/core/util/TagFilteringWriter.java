package com.composum.sling.core.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * a filter reader implementation to filter out HTML tags from the base reader
 */
public class TagFilteringWriter extends FilterWriter {

    public static final String[] DEFAULT_TO_RENAME = new String[]{};
    public static final String[] DEFAULT_TO_STRIP = new String[]{"html", "body"};
    public static final String[] DEFAULT_TO_DROP = new String[]{"head", "title", "meta", "link", "style", "script"};
    public static final String[] DEFAULT_TO_CLOSE = new String[]{"meta", "link", "input"};

    interface TagFilter {

        enum State {taghead, attribute, tagbody, tagtail}

        void setState(State state);

        void writeHead(@NotNull String snippet) throws IOException;

        void writeBody(@NotNull String snippet) throws IOException;

        void writeTail(@Nullable String snippet) throws IOException;

        boolean write(char chr) throws IOException;

        void flush() throws IOException;
    }

    /**
     * the abstract tag handler base implementation
     */
    protected class PassTagFilter implements TagFilter {

        protected final TagFilter parent;
        protected String tagName;

        protected State state = State.taghead;
        protected int token = 0;

        public PassTagFilter(@Nullable TagFilter parent, @NotNull final String tagName)
                throws IOException {
            this.parent = parent;
            this.tagName = tagName;
            writeHead("<" + tagName);
        }

        public void setState(State state) {
            this.state = state;
        }

        public void writeHead(@NotNull final String snippet) throws IOException {
            if (parent != null) {
                parent.writeBody(snippet);
            } else {
                TagFilteringWriter.this.pass(snippet);
            }
        }

        public void writeBody(@NotNull String snippet) throws IOException {
            if (parent != null) {
                parent.writeBody(snippet);
            } else {
                TagFilteringWriter.this.pass(snippet);
            }
        }

        public void writeTail(@Nullable String snippet) throws IOException {
            if (snippet == null) {
                snippet = StringUtils.isNotBlank(tagName) ? ("</" + tagName + ">") : ">";
            }
            if (parent != null) {
                parent.writeBody(snippet);
            } else {
                TagFilteringWriter.this.pass(snippet);
            }
        }

        public boolean write(char chr) throws IOException {
            switch (state) {
                case attribute:
                    writeHead("" + chr);
                    if (chr == token) {
                        state = State.taghead;
                        token = 0;
                    }
                    break;
                case taghead:
                    switch (chr) {
                        case '"':
                        case '\'':
                            state = State.attribute;
                            token = chr;
                            writeHead("" + chr);
                            break;
                        case '>':
                            if (autoClose.contains(tagName)) {
                                writeHead(">");
                                return true;
                            } else {
                                state = State.tagbody;
                            }
                        default:
                            writeHead("" + chr);
                            break;
                        case '/':
                            writeHead("" + chr);
                            state = State.tagtail;
                            tagName = "";
                            break;
                    }
                    break;
                case tagbody:
                    writeBody("" + chr);
                    break;
                case tagtail:
                    if (chr == '>') {
                        writeTail(null);
                        return true;
                    }
                    break;
            }
            return false;
        }

        public void flush() throws IOException {
        }
    }

    /**
     * the tag filter implementation for tags to 'stripe'
     */
    protected class StripTagFilter extends PassTagFilter {

        public StripTagFilter(@Nullable TagFilter parent, @NotNull final String tagName)
                throws IOException {
            super(parent, tagName);
        }

        @Override
        public void writeHead(@NotNull final String snippet) {
        }

        @Override
        public void writeTail(@Nullable final String snippet) {
        }
    }

    /**
     * the tag filter implementation for tags to 'drop'
     */
    protected class DropTagFilter extends StripTagFilter {

        public DropTagFilter(@Nullable TagFilter parent, @NotNull final String tagName)
                throws IOException {
            super(parent, tagName);
        }

        @Override
        public void writeBody(@NotNull String snippet) {
        }
    }

    public class TagWriter {

        protected final TagWriter parent;
        protected StringBuilder tagName = new StringBuilder();
        protected TagFilter tagFilter = null;

        public TagWriter(@Nullable final TagWriter parent) {
            this.parent = parent;
        }

        public void write(int chr) throws IOException {
            switch (chr) {
                case '<':
                    filterStack.push(new TagWriter(this));
                    break;
                case ' ':
                case '/':
                    if (tagName.length() == 0) {
                        filterStack.pop();
                        TagWriter parent = filterStack.peek();
                        if (parent.tagFilter != null) {
                            parent.tagFilter.setState(TagFilter.State.tagtail);
                            return;
                        }
                    }
                case '>':
                    if (tagFilter == null) {
                        tagFilter = createFilter(parent != null ? parent.tagFilter : null, tagName.toString());
                    }
                default:
                    if (tagFilter == null) {
                        tagName.append((char) chr);
                    } else {
                        if (tagFilter.write((char) chr)) {
                            filterStack.pop();
                        }
                    }
                    break;
            }
        }

        public void flush() throws IOException {
            if (tagFilter == null) {
                pass("<" + tagName);
            } else {
                tagFilter.flush();
            }
        }
    }

    public class TextWriter extends TagWriter {

        public TextWriter(@Nullable final TagWriter parent) {
            super(parent);
        }

        @Override
        public void write(int chr) throws IOException {
            switch (chr) {
                case '<':
                    filterStack.push(new TagWriter(this));
                    break;
                default:
                    pass(chr);
            }
        }

        @Override
        public void flush() throws IOException {
        }
    }

    protected final Writer wrappedWriter;
    protected final Map<String, String> toRename;
    protected final List<String> toStrip;
    protected final List<String> toDrop;
    protected final List<String> autoClose;

    protected Stack<TagWriter> filterStack = new Stack<>();

    protected TagFilter createFilter(@Nullable final TagFilter parent, @NotNull final String tagName)
            throws IOException {
        final String newTagName = toRename.get(tagName.toLowerCase());
        if (StringUtils.isNotBlank(newTagName)) {
            return new PassTagFilter(parent, newTagName);
        }
        if (toStrip.contains(tagName.toLowerCase())) {
            return new StripTagFilter(parent, tagName);
        }
        if (toDrop.contains(tagName.toLowerCase())) {
            return new DropTagFilter(parent, tagName);
        }
        return new PassTagFilter(parent, tagName);
    }

    /**
     * Creates a tag filtering reader to filter out the tags configured as sets of tag names
     *
     * @param in        the reader to filter during read
     * @param toRename  the set of tag names which should be kept but renamed (e.g. 'html:div' to keep the root)
     * @param toStrip   the set of tag names to 'stripe' - remove the tags around and keep the body of the tags
     * @param toDrop    the set of tag names to 'drop' - remove the tags including their body
     * @param autoClose the set of tag names to close automatically
     */
    public TagFilteringWriter(@NotNull final Writer writer, @NotNull final String[] toRename,
                              @NotNull final String[] toStrip, @NotNull final String[] toDrop,
                              @NotNull String[] autoClose) {
        super(writer);
        wrappedWriter = writer;
        this.toRename = new HashMap<>();
        for (String rule : toRename) {
            if (StringUtils.isNotBlank(rule)) {
                String[] split = StringUtils.split(rule, ":", 2);
                this.toRename.put(split[0], split[1]);
            }
        }
        this.toStrip = Arrays.asList(toStrip);
        this.toDrop = Arrays.asList(toDrop);
        this.autoClose = Arrays.asList(autoClose);
        filterStack.push(new TextWriter(null));
    }

    public TagFilteringWriter(@NotNull final OutputStream out, @NotNull final String[] toRename,
                              @NotNull final String[] toStrip, @NotNull String[] toDrop,
                              @NotNull String[] autoClose) {
        this(new OutputStreamWriter(out, StandardCharsets.UTF_8), toRename, toStrip, toDrop, autoClose);
    }

    public TagFilteringWriter(@NotNull final OutputStream out) {
        this(out, DEFAULT_TO_RENAME, DEFAULT_TO_STRIP, DEFAULT_TO_DROP, DEFAULT_TO_CLOSE);
    }

    public TagFilteringWriter(@NotNull final Writer writer) {
        this(writer, DEFAULT_TO_RENAME, DEFAULT_TO_STRIP, DEFAULT_TO_DROP, DEFAULT_TO_CLOSE);
    }

    //
    // Writer...
    //

    @Override
    public void flush() throws IOException {
        while (!filterStack.isEmpty()) {
            filterStack.peek().flush();
            filterStack.pop();
        }
        super.flush();
    }

    @Override
    public void write(int chr) throws IOException {
        filterStack.peek().write(chr);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        write(str.toCharArray(), off, len);
    }

    /**
     * uses the single token read() method to fill the buffer
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int count = 0; count < len; count++) {
            write(cbuf[off + count]);
        }
    }

    //
    // final write
    //

    public void pass(String str) throws IOException {
        wrappedWriter.write(str);
    }

    public void pass(String str, int off, int len) throws IOException {
        wrappedWriter.write(str, off, len);
    }

    public void pass(char[] cbuf, int off, int len) throws IOException {
        wrappedWriter.write(cbuf, off, len);
    }

    public void pass(int chr) throws IOException {
        wrappedWriter.write(chr);
    }
}
