package com.composum.sling.nodes.browser;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.nodes.components.codeeditor.CodeEditorServlet;
import com.composum.sling.nodes.console.ConsoleServletBean;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class Browser extends ConsoleServletBean {

    private static final Logger LOG = getLogger(Browser.class);

    public static final String TYPE_FILE = "nt:file";
    public static final String TYPE_RESOURCE = "nt:resource";
    public static final String OAK_RESOURCE = "oak:Resource";
    public static final String TYPE_UNSTRUCTURED = "nt:unstructured";

    public static final String HTML = "html";
    public static final String JSP = "jsp";

    public static final String PROP_DATA = "jcr:data";
    public static final String PROP_MIME_TYPE = "jcr:mimeType";
    public static final String DEFAULT_MIME_TYPE = "text/html";

    public static final Map<String, String> EDITOR_MODES;

    static {
        EDITOR_MODES = new HashMap<>();
        EDITOR_MODES.put("json", "json");
        EDITOR_MODES.put("xml", "xml");
        EDITOR_MODES.put(HTML, HTML);
        EDITOR_MODES.put(JSP, JSP);
        EDITOR_MODES.put("esp", "jsp");
        EDITOR_MODES.put("css", "css");
        EDITOR_MODES.put("less", "less");
        EDITOR_MODES.put("js", "javascript");
        EDITOR_MODES.put("ecma", "javascript");
        EDITOR_MODES.put("javascript", "javascript");
        EDITOR_MODES.put("dart", "dart");
        EDITOR_MODES.put("groovy", "groovy");
        EDITOR_MODES.put("java", "java");
        EDITOR_MODES.put("scala", "scala");
        EDITOR_MODES.put("markdown", "markdown");
        EDITOR_MODES.put("text", "text");
        EDITOR_MODES.put("txt", "text");
    }

    private transient String primaryType;
    private transient String resourceType;
    private transient String mimeType;
    private transient String nameExtension;

    private transient String viewType;
    private transient String textType;

    private transient Boolean isFile;
    private transient Boolean isAsset;
    private transient Boolean isImage;
    private transient Boolean isVideo;
    private transient Boolean isText;
    private transient Boolean isRenderable;

    private transient NodeHandle current;
    private transient NodeHandle parent;
    private transient List<NodeHandle> parents;

    public Browser(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public Browser(BeanContext context) {
        super(context);
    }

    public Browser() {
        super();
    }

    public String getPrimaryType() {
        if (primaryType == null) {
            primaryType = "";
            try {
                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    NodeType type = node.getPrimaryNodeType();
                    if (type != null) {
                        primaryType = type.getName();
                    } else {
                        primaryType = "{untyped}";
                    }
                } else {
                    primaryType = "{no node}";
                }
            } catch (RepositoryException ex) {
                primaryType = String.format("{%s}", ex.getMessage());
                LOG.error(ex.getMessage(), ex);
            }
        }
        return primaryType;
    }

    /*
     * Node/Resource - Content Type
     */

    /**
     * the content resource type (sling:resourceType) declared for the current resource
     */
    public String getResourceType() {
        if (resourceType == null) {
            resourceType = "";
            String type = resource.getResourceType();
            if (StringUtils.isBlank(type) || getPrimaryType().equals(type)) {
                Resource contentResource = getContentResource();
                if (contentResource != null) {
                    type = contentResource.getResourceType();
                    if (StringUtils.isNotBlank(type)) {
                        ResourceHandle handle = ResourceHandle.use(contentResource);
                        if (type.equals(handle.getPrimaryType())) {
                            type = null;
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(type)) {
                // check for a real existing resource type
                if (!Resource.RESOURCE_TYPE_NON_EXISTING.equals(type)) {
                    resourceType = type;
                }
            }
        }
        return resourceType;
    }

    /**
     * returns 'true' if the current resource has a well known resource type
     */
    public boolean isTyped() {
        return StringUtils.isNotBlank(getResourceType());
    }

    public boolean isFile() {
        if (isFile == null) {
            isFile = false;
            ResourceHandle contentResource = getContentResource();
            if (contentResource == null) {
                contentResource = resource; // use node itself if no content present (only in the Browser!)
            }
            if (contentResource != null) {
                ValueMap values = contentResource.adaptTo(ValueMap.class);
                if (values != null) {
                    String typeName = values.get(JcrConstants.JCR_PRIMARYTYPE, "");
                    if (TYPE_RESOURCE.equals(typeName)
                            || OAK_RESOURCE.equals(typeName)
                            || TYPE_FILE.equals(typeName)
                            || TYPE_UNSTRUCTURED.equals(typeName)) {
                        if (values.containsKey(JcrConstants.JCR_DATA)) {
                            isFile = true;
                        } else {
                            mimeType = contentResource.getProperty(PROP_MIME_TYPE);
                            if (StringUtils.isNotBlank(mimeType)) {
                                isFile = true;
                            }
                        }
                    }
                }
            }
        }
        return isFile;
    }

    public boolean isAsset() {
        if (isAsset == null) {
            isAsset = ResourceUtil.isResourceType(resource, "cpa:Asset");
        }
        return isAsset;
    }

    public boolean isImage() {
        if (isImage == null) {
            isImage = (isFile() && getMimeType().startsWith("image/")) || isAsset();
        }
        return isImage;
    }

    public String getImageCSS() {
        final String mimeType = getMimeType();
        return mimeType.substring(mimeType.indexOf('/') + 1).replaceAll("[+]", " ");
    }

    public boolean isVideo() {
        if (isVideo == null) {
            isVideo = isFile() && getMimeType().startsWith("video/");
        }
        return isVideo;
    }

    public boolean isText() {
        if (isText == null) {
            isText = isFile() && StringUtils.isNotBlank(getTextType());
        }
        return isText;
    }

    public boolean isRenderable() {
        if (isRenderable == null) {
            String extension = getNameExtension();
            isRenderable = isTyped() || (isText() &&
                    (HTML.equals(extension) /*|| JSP.equals(extension)*/));
        }
        return isRenderable;
    }

    public String getNameExtension() {
        if (nameExtension == null) {
            nameExtension = ResourceUtil.getNameExtension(getResource());
        }
        return nameExtension;
    }

    /**
     * Determines the text type for the current node using the mimeType (if present) and the extension.
     */
    public String getTextType() {
        if (textType == null) {
            String mimeType = getMimeType();
            String extension = getNameExtension();
            textType = getTextType(mimeType, extension);
        }
        return textType;
    }

    /**
     * Determines the text type for the current node using the mimeType (if present) and the extension.
     *
     * @return the type of the text file (script language) or ""
     */
    public static String getTextType(String mimeType, String extension) {
        String textType = null;
        if (StringUtils.isNotBlank(mimeType)) {
            textType = EDITOR_MODES.get(mimeType);
            if (StringUtils.isBlank(textType)) {
                String[] parts = StringUtils.split(mimeType, '/');
                if (parts.length > 1) {
                    textType = EDITOR_MODES.get(parts[1]);
                }
                if (StringUtils.isBlank(textType)) {
                    if (StringUtils.isNotBlank(extension)) {
                        textType = EDITOR_MODES.get(extension);
                    }
                    if (StringUtils.isBlank(textType)) {
                        textType = EDITOR_MODES.get(parts[0]);
                    }
                }
            }
        }
        if (StringUtils.isBlank(textType)) {
            if (StringUtils.isNotBlank(extension)) {
                textType = EDITOR_MODES.get(extension);
            }
        }
        if (textType == null) {
            textType = "";
        }
        return textType;
    }

    public String getViewType() {
        if (viewType == null) {
            viewType = "something";
            if (isFile()) {
                if (isVideo()) {
                    viewType = "video";
                } else if (isImage()) {
                    viewType = "image";
                } else if (isText()) {
                    if ("groovy".equalsIgnoreCase(getTextType())) {
                        viewType = "script";
                    } else {
                        viewType = "text";
                    }
                } else {
                    viewType = "binary";
                }
            } else {
                if (isAsset()) {
                    viewType = "image";
                } else {
                    String resourceType = getResourceType();
                    if (StringUtils.isNotBlank(resourceType)) {
                        viewType = "typed";
                    }
                }
            }
        }
        return viewType;
    }

    public String getTabType() {
        String selector = getRequest().getSelectors(new StringFilter.BlackList("^tab$"));
        return StringUtils.isNotBlank(selector) ? selector.substring(1) : "properties";
    }

    public String getName() {
        return getResource().getResourceName();
    }

    /**
     * a JSP bean for a node (for the current node)
     */
    public class NodeHandle {

        protected ResourceHandle resource;

        protected String name;
        protected String path;

        protected String pathUrl;
        protected String mappedUrl;
        protected String url;

        protected String mimeType;

        public NodeHandle(Resource resource) {
            this.resource = ResourceHandle.use(resource);
        }

        public ResourceHandle getResource() {
            return resource;
        }

        public String getId() {
            return resource.getId();
        }

        public String getName() {
            if (name == null) {
                name = resource.getName();
                if (StringUtils.isBlank(name)) {
                    name = "jcr:root";
                }
            }
            return name;
        }

        public String getNameEscaped() {
            return StringEscapeUtils.escapeHtml4(getName());
        }

        public String getPath() {
            if (path == null) {
                path = resource.getPath();
            }
            return path;
        }

        public String getPathEncoded() {
            return LinkUtil.encodePath(getPath());
        }

        /**
         * Returns a URL to the nodes View based on the nodes path (without mapping).
         */
        public String getPathUrl() {
            if (pathUrl == null) {
                pathUrl = getPathEncoded();
                pathUrl += LinkUtil.getExtension(resource, Browser.this.isAsset() ? "" : null);
            }
            return pathUrl;
        }

        /**
         * Returns a URL to the nodes View with resolver mapping.
         */
        public String getMappedUrl() {
            if (mappedUrl == null) {
                mappedUrl = LinkUtil.getMappedUrl(getRequest(), getPath());
            }
            return mappedUrl;
        }

        /**
         * Returns a URL to the nodes View probably with resolver mapping.
         */
        public String getUrl() {
            if (url == null) {
                url = LinkUtil.getUrl(getRequest(), getPath(), Browser.this.isAsset() ? "" : null);
            }
            return url;
        }

        /**
         * the content mime type declared for the current resource
         */
        public String getMimeType() {
            if (mimeType == null) {
                mimeType = MimeTypeUtil.getMimeType(resource, DEFAULT_MIME_TYPE);
            }
            return mimeType;
        }
    }

    /**
     * Returns a JSP handle of the node displayed currently by the browser.
     */
    public NodeHandle getCurrent() {
        if (current == null) {
            current = new NodeHandle(resource);
        }
        return current;
    }

    /**
     * Returns a URL to the current nodes View based on the nodes path (without mapping).
     */
    public String getCurrentPathUrl() {
        return getCurrent().getPathUrl();
    }

    /**
     * Returns a URL to the current nodes View with resolver mapping.
     */
    public String getMappedUrl() {
        return getCurrent().getMappedUrl();
    }

    /**
     * Returns a URL to the current nodes View with mapping according to the configuration.
     */
    public String getCurrentUrl() {
        return getCurrent().getUrl();
    }

    /**
     * Returns a URL to the current nodes View with mapping according to the configuration.
     */
    public String getEditCodeUrl() {
        return LinkUtil.getUnmappedUrl(getRequest(), CodeEditorServlet.SERVLET_PATH + ".html" + getCurrent().getPath());
    }

    /**
     * the content mime type declared for the current resource
     */
    public String getMimeType() {
        return getCurrent().getMimeType();
    }

    public NodeHandle getParent() {
        if (parent == null) {
            parent = new NodeHandle(resource.getParent());
        }
        return parent;
    }

    public List<NodeHandle> getParents() {
        if (parents == null) {
            parents = new ArrayList<>();
            ResourceHandle resource = getResource();
            String parentPath;
            NodeHandle parent;
            while (StringUtils.isNotBlank(parentPath = resource.getParentPath())) {
                parent = new NodeHandle(getResolver().resolve(parentPath));
                parents.add(0, parent);
                resource = parent.getResource();
            }
        }
        return parents;
    }
}
