package com.composum.sling.nodes.servlet;

import com.composum.sling.core.servlet.NodeTreeServlet;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.tika.io.IOUtils;
import org.apache.tika.mime.MimeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=" + RuntimeFileServlet.SERVLET_LABEL,
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + RuntimeFileServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        }
)
public class RuntimeFileServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeFileServlet.class);

    public static final String SERVLET_LABEL = "Composum Runtime File Servlet";
    public static final String SERVLET_PATH = "/bin/cpm/system/file";

    public static final String SA_SESSIONS = RuntimeFileServlet.class.getName() + "#session";

    public static final String RUNTIME_ROOT = new File(".").getAbsolutePath();

    public static class RuntimeFile implements Serializable {

        private final File file;
        private final String name;
        private final String path;
        private final String uri;
        private final MimeType mimeType;
        private final boolean isText;
        private final boolean isLog;

        public RuntimeFile(@NotNull final SlingHttpServletRequest request, @NotNull final File file) {
            this.file = file;
            String path = file.getAbsolutePath();
            if (path.startsWith(RUNTIME_ROOT)) {
                path = path.substring(RUNTIME_ROOT.length());
            }
            if (StringUtils.isBlank(path)) {
                path = "/";
            }
            this.path = path;
            name = "/".equals(this.path) ? "/" : file.getName();
            if (this.file.isFile()) {
                mimeType = MimeTypeUtil.getMimeType(name.replaceAll("\\.log\\.[\\d-]+$", ".log"));
                uri = request.getContextPath() + SERVLET_PATH +
                        (mimeType != null ? mimeType.getExtension() : ".bin") + this.path;
            } else {
                mimeType = null;
                uri = null;
            }
            isText = mimeType != null && mimeType.toString().startsWith("text/");
            isLog = isText && name.matches(".*\\.log(\\.[\\d-]+)?$");
        }

        @NotNull
        public File getFile() {
            return file;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public String getPath() {
            return path;
        }

        @Nullable
        public String getUri() {
            return uri;
        }

        @Nullable
        public MimeType getMimeType() {
            return mimeType;
        }

        public boolean isText() {
            return isText;
        }

        public boolean isLog() {
            return isLog;
        }
    }

    @Nullable
    protected RuntimeFile getFile(@NotNull final SlingHttpServletRequest request, @Nullable final String suffix) {
        RuntimeFile rtFile = null;
        if (suffix != null) {
            final File file = new File("." + suffix);
            if (file.exists()) {
                rtFile = new RuntimeFile(request, file);
            }
        }
        return rtFile;
    }

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws IOException {
        final RequestPathInfo pathInfo = request.getRequestPathInfo();
        final String suffix = pathInfo.getSuffix();
        final String[] selectors = pathInfo.getSelectors();
        if (selectors.length > 0) {
            switch (selectors[0]) {
                case "tree":
                    treeNode(request, response, suffix);
                    return;
                case "tail":
                    tailLogfile(request, response, selectors, suffix);
                    return;
                default:
                    break;
            }
        }
        downloadFile(request, response, suffix);
    }

    protected void downloadFile(@NotNull final SlingHttpServletRequest request,
                                @NotNull final SlingHttpServletResponse response,
                                @Nullable final String suffix) throws IOException {
        RuntimeFile rfFile = getFile(request, suffix);
        final File file;
        if (rfFile != null && (file = rfFile.getFile()).isFile() && file.canRead()) {
            final MimeType mimeType = rfFile.getMimeType();
            response.setContentType(mimeType != null ? mimeType.toString() : "text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=" + rfFile.getName());
            response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, file.lastModified());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (FileInputStream in = new FileInputStream(file)) {
                IOUtils.copy(in, response.getOutputStream());
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    //
    //  tail (logfile)
    //

    protected void tailLogfile(@NotNull final SlingHttpServletRequest request,
                               @NotNull final SlingHttpServletResponse response,
                               @NotNull final String[] selectors,
                               @Nullable final String suffix) throws IOException {
        LoggerSession loggerSession = getLoggerSession(request, suffix);
        if (loggerSession != null) {
            final Long position = selectors.length > 1 ? Long.parseLong(selectors[1]) : null;
            final Long limit = selectors.length > 2 ? Long.parseLong(selectors[2]) : null;
            final String filter = request.getParameter("filter");
            response.setContentType("text/plain");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            loggerSession.tail(response.getWriter(), position, limit, StringUtils.isNotBlank(filter)
                    ? Pattern.compile(filter.replaceAll("([^ .\\])])?[*]", "$1\\\\*"))
                    : null);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    public static class LoggerSession implements Serializable {

        private final RuntimeFile rtFile;
        private long lastPosition = 0;

        public LoggerSession(@NotNull final RuntimeFile rtFile) {
            this.rtFile = rtFile;
        }

        public synchronized void tail(@NotNull final PrintWriter writer, @Nullable Long position,
                                      @Nullable final Long limit, @Nullable final Pattern filter) {
            if (position == null) {
                position = lastPosition;
            }
            final File file = rtFile.getFile();
            final long length = file.length();
            if (position < length) {
                try (final RandomAccessFile fileAccess = new RandomAccessFile(file, "r")) {
                    if (limit != null && limit > 0) {
                        // retrive the position for the last 'limit' lines...
                        long count = limit;
                        long limitStart = length;
                        while (count > 0 && limitStart > position) {
                            fileAccess.seek(--limitStart);
                            if (fileAccess.readByte() == 0xA) {
                                count--;
                            }
                        }
                        while (limitStart > 0) {
                            fileAccess.seek(--limitStart);
                            if (fileAccess.readByte() == 0xA) {
                                limitStart++;
                                break;
                            }
                        }
                        if (limitStart > position) {
                            position = limitStart; // use found position
                        }
                    }
                    fileAccess.seek(position);
                    String line;
                    while ((line = fileAccess.readLine()) != null) {
                        if (filter == null || filter.matcher(line).find()) {
                            writer.println(line);
                        }
                    }
                    lastPosition = fileAccess.getFilePointer();
                } catch (IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
    }

    protected LoggerSession getLoggerSession(@NotNull final SlingHttpServletRequest request,
                                             @Nullable final String path) {
        LoggerSession loggerSession = null;
        final HttpSession httpSession;
        if (StringUtils.isNotBlank(path) && (httpSession = request.getSession(true)) != null) {
            Map<String, LoggerSession> sessionSet = null;
            try {
                //noinspection unchecked
                sessionSet = (Map<String, LoggerSession>) httpSession.getAttribute(SA_SESSIONS);
            } catch (ClassCastException ignore) {
            }
            if (sessionSet == null) {
                httpSession.setAttribute(SA_SESSIONS, sessionSet = new HashMap<>());
            }
            try {
                loggerSession = sessionSet.get(path);
            } catch (ClassCastException ignore) {
                httpSession.setAttribute(SA_SESSIONS, sessionSet = new HashMap<>());
            }
            if (loggerSession == null) {
                final RuntimeFile rtFile = getFile(request, path);
                final File file;
                if (rtFile != null && (file = rtFile.getFile()).isFile() && file.canRead()) {
                    loggerSession = new LoggerSession(rtFile);
                    sessionSet.put(path, loggerSession);
                } else {
                    LOG.error("can't read file '{}' ({})", rtFile != null ? rtFile.getPath() : "???", path);
                }
            }
        }
        return loggerSession;
    }

    //
    // files tree
    //

    protected void treeNode(@NotNull final SlingHttpServletRequest request,
                            @NotNull final SlingHttpServletResponse response,
                            @Nullable final String suffix) throws IOException {
        final RuntimeFile rtFile = getFile(request, suffix);
        if (rtFile != null) {
            final JsonWriter writer = ResponseUtil.getJsonWriter(response);
            writer.beginObject();
            writeFileNode(request, writer, rtFile);
            writer.endObject();
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    protected void writeFileNode(@NotNull final SlingHttpServletRequest request,
                                 @NotNull final JsonWriter writer, @NotNull final RuntimeFile rtFile)
            throws IOException {
        writeFileIdentifiers(request, writer, rtFile);
        writer.name("children").beginArray();
        File[] files = rtFile.file.listFiles();
        Set<File> fileSet = new TreeSet<>(Comparator.comparing(o -> ((o.isDirectory() ? "0:" : "1:") + o.getName())));
        fileSet.addAll(Arrays.asList(files != null ? files : new File[0]));
        for (File child : fileSet) {
            writer.beginObject();
            writeFileIdentifiers(request, writer, new RuntimeFile(request, child));
            writer.name("state").beginObject(); // that's the 'jstree' state object
            writer.name("loaded").value(false);
            writer.endObject();
            writer.endObject();
        }
        writer.endArray();
    }

    protected void writeFileIdentifiers(@NotNull final SlingHttpServletRequest request,
                                        @NotNull final JsonWriter writer, @NotNull final RuntimeFile rtFile)
            throws IOException {
        final File file = rtFile.getFile();
        writer.name("id").value(rtFile.getPath());
        writer.name("name").value(rtFile.getName());
        writer.name("text").value(rtFile.getName());
        writer.name("path").value(rtFile.getPath());
        MimeType mimeType = rtFile.getMimeType();
        if (mimeType != null) {
            writer.name("type").value("file-" + NodeTreeServlet.getMimeTypeKey(mimeType.toString()));
            writer.name("mimeType").value(mimeType.toString());
            writer.name("isText").value(rtFile.isText);
            writer.name("isLog").value(rtFile.isLog);
        } else {
            writer.name("type").value(file.isDirectory() ? "folder" : "file");
        }
        if (file.isFile()) {
            writer.name("size").value(file.length());
            writer.name("modified").value(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(file.lastModified())));
        }
        if (StringUtils.isNotBlank(rtFile.getUri())) {
            writer.name("uri").value(rtFile.getUri());
        }
    }
}
