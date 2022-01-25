package com.composum.sling.nodes.servlet;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.service.RestrictedService;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.util.ResponseUtil;
import com.composum.sling.nodes.NodesConfiguration;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.composum.sling.nodes.servlet.VersionServlet.SERVICE_KEY;

/**
 * @author Mirko Zeibig
 * @since 28.09.2015
 */
@Component(service = {Servlet.class, RestrictedService.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Version Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + VersionServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_PUT,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                "sling.auth.requirements=" + VersionServlet.SERVLET_PATH
        })
@Restricted(key = SERVICE_KEY)
public class VersionServlet extends AbstractServiceServlet {

    public static final String SERVLET_PATH = "/bin/cpm/nodes/version";

    public static final String SERVICE_KEY = "nodes/repository/versions";

    private static final Logger LOG = LoggerFactory.getLogger(VersionServlet.class);

    @Reference
    private NodesConfiguration coreConfig;

    public enum Extension {json, html}

    public enum Operation {checkout, checkin, addlabel, deletelabel, versions, labels, version, restore, configuration, checkpoint, activity}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.versions, new GetVersions());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.labels, new GetLabels());

        // PUT
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json, Operation.addlabel, new AddLabel());
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json, Operation.restore, new RestoreVersion());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json, Operation.deletelabel, new DeleteLabel());
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json, Operation.version, new DeleteVersion());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.checkout, new CheckoutOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.checkin, new CheckinOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.checkpoint, new CheckpointOperation());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.activity, new CreateActivity());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.configuration, new CreateConfiguration());
    }

    @Override
    @NotNull
    protected ServletOperationSet<Extension, Operation> getOperations() {
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

    static class Param {
        String version;
        String label;
        String path;

        public void setVersion(String version) {
            this.version = version;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class RestoreVersion implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                final Gson gson = new Gson();
                final Param p = gson.fromJson(
                        new InputStreamReader(request.getInputStream(), MappingRules.CHARSET.name()),
                        Param.class);
                versionManager.restore(path, p.version, false);
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class DeleteVersion implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                VersionHistory versionHistory = versionManager.getVersionHistory(path);
                final Gson gson = new Gson();
                final Param p = gson.fromJson(
                        new InputStreamReader(request.getInputStream(), MappingRules.CHARSET.name()),
                        Param.class);
                versionHistory.removeVersion(p.version);
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class CreateConfiguration implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                versionManager.createConfiguration(path);
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class CreateActivity implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String activity = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                versionManager.createActivity(activity.startsWith("/") ? activity.substring(1) : activity);
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class DeleteLabel implements ServletOperation {
        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                final VersionHistory versionHistory = versionManager.getVersionHistory(path);

                final Gson gson = new Gson();
                final Param p = gson.fromJson(
                        new InputStreamReader(request.getInputStream(), MappingRules.CHARSET.name()),
                        Param.class);
                versionHistory.removeVersionLabel(p.label);
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class AddLabel implements ServletOperation {
        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resourc)
                throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                final VersionHistory versionHistory = versionManager.getVersionHistory(path);

                final Gson gson = new Gson();
                final Param p = gson.fromJson(
                        new InputStreamReader(request.getInputStream(), MappingRules.CHARSET.name()),
                        Param.class);
                versionHistory.addVersionLabel(p.version, p.label, false);
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class GetLabels implements ServletOperation {
        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws IOException, ServletException {
            try {
                final Node node = resource.adaptTo(Node.class);
                if (node == null) {
                    ResponseUtil.writeEmptyArray(response);
                } else {
                    boolean isVersionable = (node.isNodeType(NodeType.MIX_VERSIONABLE)
                            || node.isNodeType(NodeType.MIX_SIMPLE_VERSIONABLE));
                    if (!isVersionable) {
                        ResponseUtil.writeEmptyArray(response);
                    } else {
                        final ResourceResolver resolver = request.getResourceResolver();
                        final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                        final String path = AbstractServiceServlet.getPath(request);
                        final VersionManager versionManager = session.getWorkspace().getVersionManager();
                        final VersionHistory versionHistory = versionManager.getVersionHistory(path);
                        final String[] versionLabels = versionHistory.getVersionLabels();
                        final RequestParameter labelParam = request.getRequestParameter("label");
                        final String label = labelParam == null ? "" : labelParam.getString();
                        try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                            jsonWriter.beginArray();
                            for (final String e : versionLabels) {
                                if (e.startsWith(label)) {
                                    jsonWriter.value(e);
                                }
                            }
                            jsonWriter.endArray();
                        }
                    }
                }
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class GetVersions implements ServletOperation {
        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws IOException, ServletException {
            try {
                final Node node = resource.adaptTo(Node.class);
                if (node == null) {
                    ResponseUtil.writeEmptyArray(response);
                } else {
                    boolean isVersionable = (node.isNodeType(NodeType.MIX_VERSIONABLE)
                            || node.isNodeType(NodeType.MIX_SIMPLE_VERSIONABLE));
                    if (!isVersionable) {
                        ResponseUtil.writeEmptyArray(response);
                    } else {
                        final ResourceResolver resolver = request.getResourceResolver();
                        final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                        final String path = AbstractServiceServlet.getPath(request);
                        final VersionManager versionManager = session.getWorkspace().getVersionManager();
                        final VersionHistory versionHistory = versionManager.getVersionHistory(path);
                        final String currentVersion = versionManager.getBaseVersion(path).getName();
                        final VersionIterator allVersions = versionHistory.getAllVersions();
                        final List<VersionEntry> entries = new ArrayList<>();
                        while (allVersions.hasNext()) {
                            final Version version = allVersions.nextVersion();
                            final Calendar cal = version.getCreated();
                            final SimpleDateFormat dateFormat = new SimpleDateFormat(MappingRules.MAP_DATE_FORMAT);
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
                                jsonWriter.name("current").value(e.versionName.equals(currentVersion));
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
                    }
                }
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class CheckoutOperation implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                versionManager.checkout(path);
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class CheckinOperation implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                versionManager.checkin(path);
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    public static class CheckpointOperation implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         @Nullable final ResourceHandle resource)
                throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                if (versionManager.isCheckedOut(path)) {
                    versionManager.checkpoint(path);
                }
                ResponseUtil.writeEmptyArray(response);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }
}
