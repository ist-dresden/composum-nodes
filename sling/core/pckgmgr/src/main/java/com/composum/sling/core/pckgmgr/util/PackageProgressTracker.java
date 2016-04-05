package com.composum.sling.core.pckgmgr.util;

import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

/**
 * Helper methods for Package handling (VLT Package Manager)
 */
public abstract class PackageProgressTracker implements ProgressTrackerListener {

    private static final Logger LOG = LoggerFactory.getLogger(PackageUtil.class);

    public static class Item {

        public final String action;
        public final String path;
        public final String message;
        public final String error;

        public final boolean actionOnly;
        public final boolean errorDetected;

        public Item(Mode mode, String action, String path, Exception ex) {
            this.action = action;
            if (Mode.TEXT.equals(mode)) {
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

    protected int actionCount = 0;
    protected int itemCount = 0;
    protected int errorCount = 0;

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

    // JSON Output

    public static class JsonTracking extends PackageProgressTracker {

        protected final JsonWriter writer;

        public JsonTracking(SlingHttpServletResponse response) throws IOException {
            this(ResponseUtil.getJsonWriter(response));
        }

        public JsonTracking(JsonWriter writer) throws IOException {
            this.writer = writer;
        }

        @Override
        public void writePrologue() throws IOException {
            writer.beginArray();
        }

        @Override
        public void writeEpilogue() throws IOException {
            writer.endArray();
        }

        @Override
        protected void writeItem(Item item) throws IOException {
            writer.beginObject();
            writer.name("action").value(item.action);
            writer.name("value").value(item.path != null ? item.path : item.message);
            writer.name("error").value(item.error);
            writer.endObject();
        }
    }

    // HTML Output

    public static class HtmlTracking extends PackageProgressTracker {

        protected final Writer writer;

        public HtmlTracking(SlingHttpServletResponse response) throws IOException {
            this(response.getWriter());
        }

        public HtmlTracking(Writer writer) throws IOException {
            this.writer = writer;
        }

        @Override
        public void writePrologue() throws IOException {
            writer.append("<table>\n");
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
            writer.append("</table>\n");
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
        }
    }
}
