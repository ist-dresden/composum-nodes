package com.composum.sling.core.servlet;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(componentAbstract = true)
public abstract class NodeTreeServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(NodeTreeServlet.class);

    /**
     * the possible tree name options
     */
    public enum LabelType {
        name, title
    }

    /**
     * Determines the filter to use for node retrieval; scans the request for filter parameter or selector.
     */
    protected abstract ResourceFilter getNodeFilter(SlingHttpServletRequest request);

    //
    // node retrieval
    //

    public interface TreeNodeStrategy {

        Iterable<Resource> getChildren(Resource nodeResource);

        ResourceFilter getFilter();

        String getTypeKey(ResourceHandle resource);

        String getContentTypeKey(ResourceHandle resource, String prefix);
    }

    public static class DefaultTreeNodeStrategy implements TreeNodeStrategy {

        protected final ResourceFilter filter;

        public DefaultTreeNodeStrategy(ResourceFilter filter) {
            this.filter = filter;
        }

        @Override
        public Iterable<Resource> getChildren(Resource nodeResource) {
            return nodeResource.getChildren();
        }

        @Override
        public ResourceFilter getFilter() {
            return filter;
        }

        @Override
        public String getTypeKey(ResourceHandle resource) {
            String type = getPrimaryTypeKey(resource);
            if ("file".equals(type)) {
                type = getFileTypeKey(resource, "file-");
            } else if ("resource".equals(type)) {
                type = getMimeTypeKey(resource, "resource-");
            } else if (StringUtils.isBlank(type) || "unstructured".equals(type)) {
                type = getResourceTypeKey(resource, "resource-");
            }
            return type;
        }

        @Override
        public String getContentTypeKey(ResourceHandle resource, String prefix) {
            return getResourceTypeKey(resource, prefix);
        }
    }

    /**
     * creates a JSON object for the requested node (requested by the suffix);
     * this JSON response contains the node identifiers, some node type hints and
     * a list of the children of the node; this operation provides the data for
     * a tree implementation which requests the nodes on demand
     * suffix: the path to the node
     * selectors / parameters:
     * - 'label': 'name' or 'title' - selects the value to use for the nodes 'text' attribute
     * URL examples:
     * - http://host/bin/cpm/nodes/node.tree.json/path/to/the/node
     * - http://host/bin/cpm/nodes/node.tree.title.json/path/to/the/node
     * - http://host/bin/cpm/nodes/node.tree.json/path/to/the/node?label=title
     */
    public class TreeOperation implements ServletOperation {

        protected TreeNodeStrategy getNodeStrategy(SlingHttpServletRequest request) {
            return new DefaultTreeNodeStrategy(getNodeFilter(request));
        }

        protected ResourceFilter getNodeFilter(SlingHttpServletRequest request) {
            return NodeTreeServlet.this.getNodeFilter(request);
        }

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            if (!resource.isValid()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            TreeNodeStrategy strategy = getNodeStrategy(request);
            LabelType labelType = RequestUtil.getParameter(request, PARAM_LABEL,
                    RequestUtil.getSelector(request, LabelType.name));

            JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);

            response.setStatus(HttpServletResponse.SC_OK);

            writeJsonNode(jsonWriter, strategy, resource, labelType, false);
        }
    }

    /**
     * extension hook for additional filters or sorting
     */
    protected List<Resource> prepareTreeItems(ResourceHandle resource, List<Resource> items) {
        return items;
    }

    //
    // JSON helpers
    //

    public void writeJsonNode(JsonWriter writer, TreeNodeStrategy nodeStrategy,
                              ResourceHandle resource, LabelType labelType, boolean isVirtual)
            throws IOException {
        writer.beginObject();
        writeJsonNodeData(writer, nodeStrategy, resource, labelType, isVirtual);
        writer.endObject();
    }

    public void writeJsonNodeData(JsonWriter writer, TreeNodeStrategy nodeStrategy,
                                  ResourceHandle resource, LabelType labelType, boolean isVirtual)
            throws IOException {
        ResourceFilter filter = nodeStrategy.getFilter();
        writeNodeIdentifiers(writer, nodeStrategy, resource, labelType, isVirtual);
        writeNodeTreeType(writer, filter, resource, isVirtual);
        writeNodeJcrState(writer, resource);
        List<Resource> children = new ArrayList<>();
        boolean hasChildren = false;
        for (Resource child : nodeStrategy.getChildren(resource)) {
            hasChildren = true;
            if (filter.accept(child) &&
                    // filter out additional synthetic folders in addition to the 1st level '//...' nodes (AEM 6.1 !?)
                    !(ResourceUtil.containsPath(children, child) && ResourceUtil.isSyntheticResource(child))) {
                children.add(ResourceHandle.use(child));
            }
        }
        if (!hasChildren) {
            if (!isVirtual) {
                addVirtualContent(writer, nodeStrategy, resource, labelType);
            }
        } else {
            children = prepareTreeItems(resource, children);
            writer.name("children").beginArray();
            for (Resource child : children) {
                ResourceHandle handle = ResourceHandle.use(child);
                writer.beginObject();
                writeNodeIdentifiers(writer, nodeStrategy, ResourceHandle.use(child), labelType, isVirtual);
                writeNodeTreeType(writer, filter, handle, isVirtual);
                writeNodeJcrState(writer, handle);
                writer.name("state").beginObject(); // that's the 'jstree' state object
                writer.name("loaded").value(false);
                writer.endObject();
                writer.endObject();
            }
            writer.endArray();
        }
    }

    public void writeNodeTreeType(JsonWriter writer, ResourceFilter filter,
                                  ResourceHandle resource, boolean isVirtual)
            throws IOException {
        String treeType = isVirtual ? "virtual" : "";
        if (StringUtils.isBlank(treeType) &&
                filter instanceof ResourceFilter.FilterSet &&
                ((ResourceFilter.FilterSet) filter).isIntermediate(resource)) {
            treeType = "intermediate";
        }
        if (StringUtils.isNotBlank(treeType)) {
            writer.name("treeType").value(treeType);
        }
    }

    public void addVirtualContent(JsonWriter writer, TreeNodeStrategy nodeStrategy,
                                  ResourceHandle resource, LabelType labelType)
            throws IOException {
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            try {
                Property property = node.getProperty(ResourceUtil.CONTENT_NODE);
                if (PropertyType.REFERENCE == property.getType()) {
                    ResourceResolver resolver = resource.getResourceResolver();
                    String reference = property.getString();
                    Session session = node.getSession();
                    Node targetNode;
                    Resource targetResource;
                    if (StringUtils.isNotBlank(reference) &&
                            (targetNode = session.getNodeByIdentifier(reference)) != null &&
                            (targetResource = resolver.getResource(targetNode.getPath() +
                                    "/" + ResourceUtil.CONTENT_NODE)) != null) {
                        writer.name("children").beginArray();
                        writeJsonNode(writer, nodeStrategy, ResourceHandle.use(targetResource),
                                labelType, true);
                        writer.endArray();
                    }
                }
            } catch (RepositoryException ignored) {
            }
        }
    }

    public String getSortName(Resource resource) {
        String name = resource.getName().toLowerCase();
        if (name.startsWith("rep:")) {
            name = "a" + name;
        } else if (name.startsWith("jcr:")) {
            name = "b" + name;
        } else {
            name = "x" + name;
        }
        return name;
    }

    public String getNodeLabel(ResourceHandle resource, LabelType labelType) {
        String text = resource.getName();
        if (labelType == LabelType.title) {
            String title = resource.getProperty(ResourceUtil.PROP_TITLE, "");
            if (StringUtils.isBlank(title)) {
                Resource contentResource = resource.getContentResource();
                if (contentResource != null) {
                    title = ResourceHandle.use(contentResource).getProperty(ResourceUtil.PROP_TITLE, "");
                }
            }
            if (StringUtils.isNotBlank(title)) {
                text = title;
            }
        }
        if (StringUtils.isBlank(text) && "/".equals(resource.getPath())) {
            text = "jcr:root";
        }
        return text;
    }

    public String writeNodeIdentifiers(JsonWriter writer, TreeNodeStrategy nodeStrategy,
                                       ResourceHandle resource, LabelType labelType, boolean isVirtual)
            throws IOException {
        String text = getNodeLabel(resource, labelType);
        String type = nodeStrategy.getTypeKey(resource);
        String contentType = getContentTypeKey(nodeStrategy, resource, null);
        if (StringUtils.isNotBlank(contentType)) {
            writer.name("contentType").value(contentType);
            if ("designer".equals(contentType)) {
                type += "-" + contentType;
            }
        }
        if (StringUtils.isNotBlank(type)) {
            writer.name("type").value(type);
        }
        String path = resource.getPath();
        writer.name("id").value(path + (isVirtual ? "-v" : ""));
        writer.name("name").value(resource.getName());
        writer.name("text").value(text);
        writer.name("path").value(path);
        String resourceType = resource.getResourceType();
        if (StringUtils.isNotBlank(resourceType)) {
            writer.name("resourceType").value(resourceType);
        }
        String uuid = resource.getProperty(ResourceUtil.PROP_UUID);
        if (StringUtils.isNotBlank(uuid)) {
            writer.name("uuid").value(uuid);
        }
        return type;
    }

    public void writeNodeJcrState(JsonWriter writer,
                                  ResourceHandle resource)
            throws IOException {
        writer.name("jcrState").beginObject();
        try {
            Node node = resource.adaptTo(Node.class);
            if (node != null) {
                try {
                    String nodePath = node.getPath();
                    Session session = node.getSession();
                    Workspace workspace = session.getWorkspace();
                    LockManager lockManager = workspace.getLockManager();
                    JsonUtil.writeValue(writer, "checkedOut", node.isCheckedOut());
                    JsonUtil.writeValue(writer, "isVersionable", isVersionable(node));
                    boolean isLocked = node.isLocked();
                    JsonUtil.writeValue(writer, "locked", isLocked);
                    if (isLocked) {
                        writer.name("lock").beginObject();
                        try {
                            Lock lock = lockManager.getLock(nodePath);
                            String holderPath = lock.getNode().getPath();
                            JsonUtil.writeValue(writer, "isDeep", lock.isDeep());
                            JsonUtil.writeValue(writer, "isHolder", holderPath.equals(nodePath));
                            JsonUtil.writeValue(writer, "node", holderPath);
                            JsonUtil.writeValue(writer, "owner", lock.getLockOwner());
                            JsonUtil.writeValue(writer, "sessionScoped", lock.isSessionScoped());
                        } finally {
                            writer.endObject();
                        }
                    }
                } catch (RepositoryException rex) {
                    LOG.error(rex.getMessage(), rex);
                }
            }
        } finally {
            writer.endObject();
        }
    }

    private boolean isVersionable(Node node) throws RepositoryException {
        return (node.isNodeType(NodeType.MIX_VERSIONABLE) || node.isNodeType(NodeType.MIX_SIMPLE_VERSIONABLE));
    }
    // receiving JSON ...

    /**
     * the structure for parsing property values from JSON using Gson
     */
    public static class NodeParameters {

        public String type;
        public String path;
        public String before;
        public String index;
        public String name;
        public String title;
        public String mimeType;
        public String resourceType;
        public String jcrContent;

        public Integer index() {
            return StringUtils.isNotBlank(index) ? Integer.parseInt(index) : null;
        }

        public Map<String, Object> asMap() {
            return new HashMap<String, Object>() {{
                put(JcrConstants.JCR_PRIMARYTYPE, type);
                if (StringUtils.isNotBlank(mimeType)) {
                    put(JcrConstants.JCR_MIMETYPE, new String[]{mimeType});
                }
                if (StringUtils.isNotBlank(resourceType)) {
                    put(ResourceUtil.PROP_RESOURCE_TYPE, resourceType);
                }
                if (StringUtils.isNotBlank(title)) {
                    put(ResourceUtil.PROP_TITLE, title);
                }
            }};
        }
    }

    public NodeParameters getFormParameters(SlingHttpServletRequest request) {
        // copy parameters from request
        NodeParameters params = new NodeParameters();
        params.name = request.getParameter(PARAM_NAME);
        params.path = request.getParameter(PARAM_PATH);
        params.before = request.getParameter(PARAM_BEFORE);
        params.index = request.getParameter(PARAM_INDEX);
        params.type = request.getParameter(PARAM_TYPE);
        params.title = request.getParameter(PARAM_TITLE);
        params.mimeType = request.getParameter(PARAM_MIME_TYPE);
        params.resourceType = request.getParameter(PARAM_RESOURCE_TYPE);
        params.jcrContent = request.getParameter(PARAM_JCR_CONTENT);
        return params;
    }

    //
    // node type key generation
    //

    public static String getContentTypeKey(TreeNodeStrategy strategy, ResourceHandle resource, String prefix) {
        String contentType = null;
        if (!ResourceUtil.CONTENT_NODE.equals(resource.getName())) {
            resource = ResourceHandle.use(resource.getChild(ResourceUtil.CONTENT_NODE));
        }
        if (resource.isValid()) {
            contentType = strategy.getContentTypeKey(resource, prefix);
        }
        return contentType;
    }

    public static String getPrimaryTypeKey(ResourceHandle resource) {
        String type = resource.getPrimaryType();
        if (StringUtils.isNotBlank(type)) {
            int namespace = type.lastIndexOf(':');
            if (namespace >= 0) {
                type = type.substring(namespace + 1);
            }
            type = type.toLowerCase();
        }
        return type;
    }

    public static String getResourceTypeKey(ResourceHandle resource, String prefix) {
        String primaryType = resource.getPrimaryType();
        String type = null;
        if (resource.isValid()) {
            String resourceType = resource.getResourceType();
            if (StringUtils.isNotBlank(resourceType) && !resourceType.equals(primaryType)) {
                int namespace = resourceType.lastIndexOf(':');
                if (namespace >= 0) {
                    resourceType = resourceType.substring(namespace + 1);
                }
                int dot = resourceType.lastIndexOf('.');
                if (dot >= 0) {
                    resourceType = resourceType.substring(dot + 1);
                }
                type = resourceType.substring(resourceType.lastIndexOf('/') + 1);
                type = type.toLowerCase();
            }
        }
        if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(prefix)) {
            type = prefix + type;
        }
        return type;
    }

    public static String getFileTypeKey(ResourceHandle resource, String prefix) {
        String type = null;
        ResourceHandle content = ResourceHandle.use(resource.getChild(ResourceUtil.CONTENT_NODE));
        if (content.isValid()) {
            type = getMimeTypeKey(content, prefix);
        }
        return type;
    }

    public static String getMimeTypeKey(ResourceHandle resource, String prefix) {
        String type = null;
        String mimeType = MimeTypeUtil.getMimeType(resource, "");
        if (StringUtils.isNotBlank(mimeType)) {
            int delim = mimeType.indexOf('/');
            String major = mimeType.substring(0, delim);
            String minor = mimeType.substring(delim + 1);
            type = major;
            if ("text".equals(major)) {
                type += "-" + minor;
            } else if ("application".equals(major)) {
                type = minor;
            }
            type = type.toLowerCase();
        }
        if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(prefix)) {
            type = prefix + type;
        }
        return type;
    }
}
