package com.composum.sling.nodes.tools;

import com.composum.sling.core.Restricted;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.service.ServiceRestrictions;
import com.composum.sling.core.servlet.NodeTreeServlet;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.RequestUtil;
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
import org.osgi.service.component.annotations.Reference;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.sling.nodes.tools.RuntimeFileServlet.SERVICE_KEY;

@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=" + RuntimeFileServlet.SERVLET_LABEL,
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + RuntimeFileServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        }
)
@Restricted(key = SERVICE_KEY)
public class RuntimeFileServlet extends SlingSafeMethodsServlet implements RestrictedService {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeFileServlet.class);

    public static final String SERVICE_KEY = "system/runtime/files";

    public static final String SERVLET_LABEL = "Composum Runtime File Servlet";
    public static final String SERVLET_PATH = "/bin/cpm/system/file";

    public static final Pattern IS_LOGFILE = Pattern.compile("^((/.*)?/)?.+\\.log(\\.[\\d-]+)?$");

    public static final String SA_SESSIONS = RuntimeFileServlet.class.getName() + "#session";

    public static final String RUNTIME_ROOT = new File(".").getAbsolutePath();

    public static class LogfileFilter implements Serializable {

        protected final Pattern pattern;
        protected final int prepend;
        protected final int append;

        protected int more = 0;
        protected boolean matched = false;
        protected boolean skipped = false;
        protected boolean flushed = false;
        protected boolean flushBuffer = false;
        protected final List<String> buffer = new ArrayList<>();

        public LogfileFilter(@Nullable final Pattern pattern, int prepend, int append) {
            this.pattern = pattern;
            this.prepend = prepend;
            this.append = append;
        }

        public LogfileFilter(@NotNull final SlingHttpServletRequest request) {
            final String filter = request.getParameter("filter");
            final String[] around = StringUtils.split(RequestUtil.getParameter(request, "around", "3,1"), ",");
            pattern = StringUtils.isNotBlank(filter) ? Pattern.compile(filter) : null;
            prepend = around.length > 0 ? getInt(around[0], 3) : 3;
            append = around.length > 1 ? getInt(around[1], 1) : 1;
        }

        protected int getInt(@Nullable final String str, int defaultValue) {
            if (StringUtils.isNotBlank(str)) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException nfex) {
                    // ok, return default
                }
            }
            return defaultValue;
        }

        @SuppressWarnings("CopyConstructorMissesField")
        public LogfileFilter(@NotNull final RuntimeFileServlet.LogfileFilter template) {
            this(template.pattern, template.prepend, template.append);
        }

        @Override
        public String toString() {
            return (pattern != null ? pattern.toString() : "") + "${" + prepend + "," + append + "}";
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof LogfileFilter && toString().equals(other.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        public boolean isFilter() {
            return pattern != null;
        }

        public @Nullable String readLine(@NotNull final RandomAccessFile fileAccess) throws IOException {
            if (isFilter()) {
                String result = null;
                String line;
                if (!flushBuffer || (buffer.size() < 1 && more < 1)) {
                    flushBuffer = false;
                    while (!flushBuffer && (line = fileAccess.readLine()) != null) {
                        buffer.add(line);
                        if (!checkMatch(line)) {
                            if (buffer.size() > prepend) {
                                buffer.remove(0);
                                skipped = true;
                            }
                        }
                    }
                    if (flushBuffer && flushed && skipped) {
                        buffer.add(0, "----");
                    }
                }
                if (flushBuffer && (buffer.size() > 0 || more > 0)) {
                    flushed = true;
                    skipped = false;
                    if (buffer.size() < 1) {
                        more--;
                        line = fileAccess.readLine();
                        checkMatch(line);
                        return line;
                    }
                    result = buffer.get(0);
                    buffer.remove(0);
                }
                return result;
            } else {
                return fileAccess.readLine();
            }
        }

        protected boolean checkMatch(@Nullable final String line) {
            if (line != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find() || (matched && line.matches("^(Caused)?\\s+.*$"))) {
                    matched = true;
                    flushBuffer = true;
                    more = append;
                    return true;
                }
            }
            matched = false;
            return false;
        }
    }

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
            isLog = isText && IS_LOGFILE.matcher(path).matches();
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

    @Reference
    protected ServiceRestrictions restrictions;

    private ServiceRestrictions.Key serviceKey;
    private ServiceRestrictions.Permission permission;
    private boolean enabled = false;
    private Pattern fileRestrictions;

    protected void activate() {
        serviceKey = new ServiceRestrictions.Key(SERVICE_KEY);
        permission = restrictions.getPermission(serviceKey);
        enabled = permission != ServiceRestrictions.Permission.none;
        final String pattern = restrictions.getRestrictions(serviceKey);
        fileRestrictions = StringUtils.isNotBlank(pattern) ? Pattern.compile(pattern) : null;
    }

    @Override
    @NotNull
    public ServiceRestrictions.Key getServiceKey() {
        return serviceKey;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final ServiceRestrictions.Permission getPermission() {
        return permission;
    }

    @Nullable
    protected RuntimeFile getFile(@NotNull final SlingHttpServletRequest request, @Nullable final String suffix) {
        RuntimeFile rtFile = null;
        if (suffix != null) {
            final File file = new File("." + suffix);
            if (file.exists()) {
                final RuntimeFile candidate = new RuntimeFile(request, file);
                if (isEnabled() && (fileRestrictions == null
                        || fileRestrictions.matcher(candidate.getPath()).matches())) {
                    rtFile = candidate;
                }
            }
        }
        return rtFile;
    }

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response)
            throws IOException {
        if (isEnabled()) {
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
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    protected void downloadFile(@NotNull final SlingHttpServletRequest request,
                                @NotNull final SlingHttpServletResponse response,
                                @Nullable final String suffix) throws IOException {
        RuntimeFile rtFile = getFile(request, suffix);
        final File file;
        if (rtFile != null && (file = rtFile.getFile()).isFile() && file.canRead()) {
            final MimeType mimeType = rtFile.getMimeType();
            response.setContentType(mimeType != null ? mimeType.toString() : "text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=" + rtFile.getName());
            response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, file.lastModified());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            final LogfileFilter filter;
            if (rtFile.isLog && (filter = new LogfileFilter(request)).isFilter()) {
                PrintWriter writer = response.getWriter();
                try (final RandomAccessFile fileAccess = new RandomAccessFile(file, "r")) {
                    String line;
                    while ((line = filter.readLine(fileAccess)) != null) {
                        writer.println(line);
                    }
                }
            } else {
                try (FileInputStream in = new FileInputStream(file)) {
                    IOUtils.copy(in, response.getOutputStream());
                }
            }
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
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
            final String around = request.getParameter("around");
            response.setContentType("text/plain");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            loggerSession.tail(response.getWriter(), position, limit, new LogfileFilter(request));
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    public static class LoggerSession implements Serializable {

        private final RuntimeFile rtFile;
        private LogfileFilter logfileFilter;
        private long lastPosition = 0;

        public LoggerSession(@NotNull final RuntimeFile rtFile, @NotNull final RuntimeFileServlet.LogfileFilter filter) {
            this.rtFile = rtFile;
            this.logfileFilter = filter;
        }

        public synchronized void tail(@NotNull final PrintWriter writer, @Nullable Long position,
                                      @Nullable final Long limit, @NotNull final RuntimeFileServlet.LogfileFilter filter) {
            if (position == null && logfileFilter.equals(filter)) {
                position = lastPosition;
            } else {
                logfileFilter = filter; // reset state
                position = 0L;
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
                    while ((line = logfileFilter.readLine(fileAccess)) != null) {
                        writer.println(line);
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
                    loggerSession = new LoggerSession(rtFile, new LogfileFilter(request));
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
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
            RuntimeFile rtChild = new RuntimeFile(request, child);
            if (fileRestrictions == null || fileRestrictions.matcher(rtChild.getPath()).matches()) {
                writer.beginObject();
                writeFileIdentifiers(request, writer, new RuntimeFile(request, child));
                writer.name("state").beginObject(); // that's the 'jstree' state object
                writer.name("loaded").value(false);
                writer.endObject();
                writer.endObject();
            }
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
