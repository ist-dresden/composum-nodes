package com.composum.sling.nodes.components;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.service.PathReferencesService;
import com.composum.sling.core.service.PathReferencesService.Hit;
import com.composum.sling.core.service.PathReferencesService.HitIterator;
import com.composum.sling.core.service.PathReferencesService.Options;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.console.ConsoleServletBean;
import com.composum.sling.nodes.servlet.NodeServlet;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

@Restricted(key = NodeServlet.SERVICE_KEY)
public class ReferencesModel extends ConsoleServletBean {

    private transient Map<String, Hit> references;
    private transient Options options;
    protected String queryString = "";
    protected String message = "";

    public ReferencesModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ReferencesModel(BeanContext context) {
        super(context);
    }

    public ReferencesModel() {
        super();
    }

    @Override
    @NotNull
    public ResourceHandle getResource() {
        Resource resource = null;
        final SlingHttpServletRequest request = context.getRequest();
        final RequestPathInfo pathInfo = request.getRequestPathInfo();
        final String suffix = pathInfo.getSuffix();
        if (StringUtils.isNotBlank(suffix)) {
            resource = request.getResourceResolver().getResource(suffix);
        }
        return resource != null ? ResourceHandle.use(resource) : super.getResource();
    }

    public boolean isHasHits() {
        return !getReferences().isEmpty();
    }

    @NotNull
    public Iterable<Hit> getHits() {
        return getReferences().values();
    }

    @NotNull
    public Map<String, Hit> getReferences() {
        if (references == null) {
            references = new TreeMap<>();
            try {
                final PathReferencesService service = context.getService(PathReferencesService.class);
                if (service != null) {
                    options = getOptions();
                    final SlingHttpServletRequest request = context.getRequest();
                    final String searchRoot = XSS.filter(RequestUtil.getParameter(request, "root", "/content"));
                    final HitIterator found = service.findReferences(context.getResolver(), options, searchRoot, getPath());
                    Throwable exception = found.getThrowable();
                    if (exception != null) {
                        message = exception.getMessage();
                    }
                    queryString = found.getQueryString();
                    while (found.hasNext()) {
                        final Hit hit = found.next();
                        references.put(hit.getResource().getPath(), hit);
                    }
                }
            } catch (Exception ex) {
                message = ex.getMessage();
            }
        }
        return references;
    }

    public Options getOptions() {
        final SlingHttpServletRequest request = context.getRequest();
        if (options == null) {
            options = new Options()
                    .basePath(XSS.filter(RequestUtil.getParameter(request, "base", "")))
                    .primaryType(XSS.filter(RequestUtil.getParameter(request, "nt", "")))
                    .resourceName(XSS.filter(RequestUtil.getParameter(request, "rn", "")))
                    .resourceType(XSS.filter(RequestUtil.getParameter(request, "rt", "")))
                    .contentPath(XSS.filter(RequestUtil.getParameter(request, "cp", "")))
                    .propertyName(XSS.filter(RequestUtil.getParameter(request, "pn", "")))
                    .useAbsolutePath(RequestUtil.getParameter(request, "abs", true))
                    .useRelativePath(RequestUtil.getParameter(request, "rel", false))
                    .useTextSearch(RequestUtil.getParameter(request, "text", false))
                    .includeChildren(RequestUtil.getParameter(request, "ic", false))
                    .childrenOnly(RequestUtil.getParameter(request, "co", false))
                    .findRichText(RequestUtil.getParameter(request, "rich", false));

        }
        return options;
    }

    public String getOptionsJson() {
        final Options options = getOptions();
        final StringWriter buffer = new StringWriter();
        try {
            JsonWriter writer = new JsonWriter(buffer);
            writer.setIndent("    ");
            writer.setHtmlSafe(true);
            writer.beginObject();
            writer.name("useAbsolutePath").value(options.isUseAbsolutePath());
            writer.name("useRelativePath").value(options.isUseRelativePath());
            writer.name("basePath").value(options.getBasePath());
            writer.name("findRichText").value(options.isFindRichText());
            writer.name("useTextSearch").value(options.isUseTextSearch());
            writer.name("includeChildren").value(options.isIncludeChildren());
            writer.name("childrenOnly").value(options.isChildrenOnly());
            writer.name("primaryType").value(options.getPrimaryType());
            writer.name("resourceName").value(options.getResourceName());
            writer.name("contentPath").value(options.getContentPath());
            writer.name("propertyName").value(options.getPropertyName());
            writer.endObject();
            writer.flush();
        } catch (IOException ex) {
            message = ex.getMessage();
        }
        return buffer.toString();
    }

    public String getQueryString() {
        return queryString;
    }

    public String getMessage() {
        return message;
    }
}
