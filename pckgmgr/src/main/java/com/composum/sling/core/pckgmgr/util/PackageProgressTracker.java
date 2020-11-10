package com.composum.sling.core.pckgmgr.util;

import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.regex.Pattern;

/**
 * Helper methods for Package handling (VLT Package Manager)
 */
public abstract class PackageProgressTracker implements ProgressTrackerListener {

    private static final Logger LOG = LoggerFactory.getLogger(PackageUtil.class);

    public static final String PLAIN_TEXT_SHORT_ACTION_SPACE = "   ";

    public static class Item {

        public final String action;
        public final String path;
        public final String message;
        public final String error;

        public final boolean actionOnly;
        public final boolean errorDetected;

        public Item(Mode mode, String action, String path, Exception ex) {
            this.action = action;
            if (mode == Mode.TEXT) {
                this.message = path;
                this.path = null;
            } else {
                this.message = null;
                this.path = path;
            }
            this.error = ex != null ? ex.toString() : null;
            actionOnly = StringUtils.isBlank(path) && StringUtils.isBlank(message);
            errorDetected = StringUtils.isNotBlank(error);
        }
    }

    protected int actionCount;
    protected int itemCount;
    protected int errorCount;

    public boolean getErrorDetected() {
        return errorCount > 0;
    }

    @Override
    public void onMessage(Mode mode, String action, String path) {
        try {
            Item item = new Item(mode, action, path, null);
            writeItem(item);
            if (item.actionOnly) {
                actionCount++;
            } else {
                itemCount++;
            }
        } catch (IOException ex) {
            errorCount++;
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void onError(Mode mode, String path, Exception ex) {
        LOG.warn("Received error for mode {} path {}", new Object[]{mode, path, ex});
        errorCount++;
        try {
            writeItem(new Item(mode, "E", path, ex));
        } catch (IOException ioex) {
            errorCount++;
            LOG.error(ioex.getMessage(), ioex);
        }
    }

    protected abstract void writeItem(Item item) throws IOException;

    public abstract void writePrologue() throws IOException;

    public abstract void writeEpilogue() throws IOException;

    // LOG Output

    public static class LogOnlyTracking extends PackageProgressTracker {

        protected final String label;

        public LogOnlyTracking(String label) {
            this.label = label;
        }

        @Override
        public void writePrologue() {
        }

        @Override
        public void writeEpilogue() {
        }

        @Override
        protected void writeItem(Item item) {
            if (LOG.isDebugEnabled()) {
                StringBuilder builder = new StringBuilder(label);
                builder.append(" - [ action: ").append(item.action);
                builder.append(", ").append(item.path != null ? ("path: " + item.path) : ("message: " + item.message));
                if (item.errorDetected) {
                    builder.append(", error: ").append(item.error);
                }
                LOG.debug(builder.toString());
            }
        }
    }

    // Plain text Output

    public static class TextWriterTracking extends PackageProgressTracker {

        protected final PrintWriter writer;
        protected final Pattern finalizedIndicator;

        public TextWriterTracking(SlingHttpServletResponse response, Pattern finalizedIndicator)
                throws IOException {
            this(response.getWriter(), finalizedIndicator);
        }

        public TextWriterTracking(PrintWriter writer, Pattern finalizedIndicator) {
            this.writer = writer;
            this.finalizedIndicator = finalizedIndicator;
        }

        @Override
        public void writePrologue() {
        }

        @Override
        public void writeEpilogue() throws IOException {
        }

        @Override
        protected void writeItem(Item item) throws IOException {
            if (item.action != null) {
                int len = item.action.length();
                if (len < PLAIN_TEXT_SHORT_ACTION_SPACE.length()) {
                    writer.append("  ");
                    writer.append(item.action);
                    writer.append(PLAIN_TEXT_SHORT_ACTION_SPACE.substring(len));
                } else {
                    writer.append(item.action);
                    writer.append(' ');
                }
            } else {
                writer.append(PLAIN_TEXT_SHORT_ACTION_SPACE);
            }
            writer.append(item.path != null ? item.path : item.message);
            if (item.errorDetected) {
                writer.append("\n  ! ");
                writer.append(item.error);
            }
            writer.append('\n');
            if (finalizedIndicator != null && item.action != null &&
                    finalizedIndicator.matcher(item.action).matches()) {
                writeEpilogue();
            }
        }
    }

    // JSON Output

    public static class JsonStreamTracking extends PackageProgressTracker {

        protected final JsonWriter writer;
        protected final Pattern finalizedIndicator;

        public JsonStreamTracking(SlingHttpServletResponse response, Pattern finalizedIndicator)
                throws IOException {
            this(ResponseUtil.getJsonWriter(response), finalizedIndicator);
        }

        public JsonStreamTracking(JsonWriter writer, Pattern finalizedIndicator) {
            this.writer = writer;
            this.finalizedIndicator = finalizedIndicator;
        }

        @Override
        public void writePrologue() throws IOException {
        }

        @Override
        public void writeEpilogue() throws IOException {
        }

        @Override
        protected void writeItem(Item item) throws IOException {
            writer.beginObject();
            writer.name("action").value(item.action);
            writer.name("value").value(item.path != null ? item.path : item.message);
            writer.name("error").value(item.error);
            writer.endObject();
            if (finalizedIndicator != null && item.action != null &&
                    finalizedIndicator.matcher(item.action).matches()) {
                writeEpilogue();
            }
        }
    }

    public static class JsonTracking extends JsonStreamTracking {

        public JsonTracking(SlingHttpServletResponse response, Pattern finalizedIndicator)
                throws IOException {
            super(response, finalizedIndicator);
        }

        public JsonTracking(JsonWriter writer, Pattern finalizedIndicator) {
            super(writer, finalizedIndicator);
        }

        @Override
        public void writePrologue() throws IOException {
            writer.beginArray();
            super.writePrologue();
        }

        @Override
        public void writeEpilogue() throws IOException {
            super.writeEpilogue();
            writer.endArray();
        }
    }

    // HTML Output

    public static class HtmlStreamTracking extends PackageProgressTracker {

        protected final Writer writer;
        protected final Pattern finalizedIndicator;

        public HtmlStreamTracking(SlingHttpServletResponse response, Pattern finalizedIndicator)
                throws IOException {
            this(response.getWriter(), finalizedIndicator);
        }

        public HtmlStreamTracking(Writer writer, Pattern finalizedIndicator) {
            this.writer = writer;
            this.finalizedIndicator = finalizedIndicator;
        }

        @Override
        public void writePrologue() throws IOException {
        }

        @Override
        public void writeEpilogue() throws IOException {
            writer.append("<tr class=\"summary ");
            writer.append(getErrorDetected() ? "with-errors" : "successful");
            writer.append("\"><td colspan=\"3\">");
            writer.append("Finished ").append(getErrorDetected() ? "with errors" : "successfully");
            writer.append(" - items: ").append(Integer.toString(itemCount));
            writer.append(", actions: ").append(Integer.toString(actionCount));
            writer.append(", errors: ").append(Integer.toString(errorCount));
            writer.append("</td></tr>\n");
        }

        @Override
        protected void writeItem(Item item) throws IOException {
            writer.append("<tr class=\"").append(item.errorDetected ? "error" : "success").append("\">");
            writer.append("<td class=\"action");
            if (item.actionOnly) {
                writer.append(" no-message\" colspan=\"").append(item.errorDetected ? "2" : "3");
            }
            writer.append("\">").append(item.action).append("</td>");
            if (!item.actionOnly) {
                writer.append("<td class=\"value\"");
                if (!item.errorDetected) {
                    writer.append(" colspan=\"2\"");
                }
                writer.append(">").append(item.path != null ? item.path : item.message).append("</td>");
            }
            if (item.errorDetected) {
                writer.append("<td class=\"errmsg\">").append(item.error).append("</td>");
            }
            writer.append("</tr>\n");
            if (finalizedIndicator != null && item.action != null &&
                    finalizedIndicator.matcher(item.action).matches()) {
                writeEpilogue();
            }
        }
    }

    public static class HtmlTracking extends HtmlStreamTracking {

        public HtmlTracking(SlingHttpServletResponse response, Pattern finalizedIndicator)
                throws IOException {
            super(response, finalizedIndicator);
        }

        public HtmlTracking(Writer writer, Pattern finalizedIndicator) {
            super(writer, finalizedIndicator);
        }

        @Override
        public void writePrologue() throws IOException {
            writer.append("<table>\n");
            super.writePrologue();
        }

        @Override
        public void writeEpilogue() throws IOException {
            super.writeEpilogue();
            writer.append("</table>\n");
        }
    }
}
