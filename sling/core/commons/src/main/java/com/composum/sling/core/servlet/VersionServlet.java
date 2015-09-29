package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * @author Mirko Zeibig
 * @since 28.09.2015
 */
@SlingServlet(
        paths = "/bin/core/version",
        methods = {"GET", "PUT"}
)
public class VersionServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(VersionServlet.class);

    public enum Extension {json, html}

    public enum Operation {checkout, checkin, versions}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Reference
    private CoreConfiguration coreConfig;

    @Override protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.versions, new GetVersions());

        // PUT
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json, Operation.checkout, new CheckoutOperation());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json, Operation.checkin, new CheckinOperation());
    }

    @Override protected ServletOperationSet getOperations() {
        return operations;
    }

    static class VersionEntry {
        String versionName;
        String date;
        List<String> labels = new ArrayList<>();
        VersionEntry(String versionName, String date) {
            this.versionName = versionName;
            this.date = date;
        }
    }

    public static class GetVersions implements ServletOperation {
        @Override public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                final ResourceHandle resource) throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                final VersionHistory versionHistory = versionManager.getVersionHistory(path);
                final VersionIterator allVersions = versionHistory.getAllVersions();
                final List<VersionEntry> entries = new ArrayList<>();
                while (allVersions.hasNext()) {
                    final Version version = allVersions.nextVersion();
                    final Calendar cal = version.getCreated();
                    final SimpleDateFormat dateFormat = MappingRules.MAP_DATE_FORMAT;
                    dateFormat.setTimeZone(cal.getTimeZone());
                    final VersionEntry versionEntry = new VersionEntry(version.getName(),
                            dateFormat.format(cal.getTime()));
                    versionEntry.labels.addAll(Arrays.asList(versionHistory.getVersionLabels(version)));
                    entries.add(versionEntry);
                }
                try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                    jsonWriter.beginArray();
                    for (final VersionEntry e : entries) {
                        jsonWriter.beginObject();
                        jsonWriter.name("name").value(e.versionName);
                        jsonWriter.name("date").value(e.date);
                        jsonWriter.name("labels").beginArray();
                        for (final String l : e.labels) {
                            jsonWriter.value(l);
                        }
                        jsonWriter.endArray();
                        jsonWriter.endObject();
                    }
                    jsonWriter.endArray();
                }
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class CheckoutOperation implements ServletOperation {

        @Override public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                final ResourceHandle resource) throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                versionManager.checkout(path);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class CheckinOperation implements ServletOperation {

        @Override public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                final ResourceHandle resource) throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                versionManager.checkin(path);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }
}
