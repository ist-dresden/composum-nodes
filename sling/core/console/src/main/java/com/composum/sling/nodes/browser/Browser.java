package com.composum.sling.nodes.browser;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.HashMap;
import java.util.Map;

public class Browser extends BrowserBean {

    public static final String TYPE_FILE = "nt:file";
    public static final String TYPE_RESOURCE = "nt:resource";
    public static final String TYPE_UNSTRUCTURED = "nt:unstructured";

    public static final String HTML = "html";
    public static final String JSP = "jsp";

    public static final String PROP_DATA = "jcr:data";
    public static final String PROP_MIME_TYPE = "jcr:mimeType";

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
    private transient Boolean isImage;
    private transient Boolean isVideo;
    private transient Boolean isText;
    private transient Boolean isRenderable;

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
                ex.printStackTrace();
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
                        if (handle.getPrimaryType().equals(type)) {
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
                try {
                    Node node = contentResource.adaptTo(Node.class);
                    if (node != null) {
                        NodeType type = node.getPrimaryNodeType();
                        String typeName = type.getName();
                        if (TYPE_RESOURCE.equals(typeName)
                                || TYPE_FILE.equals(typeName)
                                || TYPE_UNSTRUCTURED.equals(typeName)) {
                            if (getData() != null) {
                                isFile = true;
                            } else {
                                mimeType = contentResource.getProperty(PROP_MIME_TYPE);
                                if (StringUtils.isNotBlank(mimeType)) {
                                    isFile = true;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return isFile;
    }

    public boolean isImage() {
        if (isImage == null) {
            isImage = isFile() && getMimeType().startsWith("image/");
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

    public Property getData() {
        Property data = null;
        ResourceHandle content = getContentResource();
        if (content != null) {
            Node node = content.adaptTo(Node.class);
            if (node != null) {
                try {
                    data = node.getProperty(PROP_DATA);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return data;
    }

    public Binary getBinary() {
        Binary binary = null;
        Property data = getData();
        if (data != null) {
            try {
                binary = data.getBinary();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
        return binary;
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
                String resourceType = getResourceType();
                if (StringUtils.isNotBlank(resourceType)) {
                    viewType = "typed";
                }
            }
        }
        return viewType;
    }

    public String getTabType() {
        String selector = getRequest().getSelectors(new StringFilter.BlackList("^tab$"));
        return StringUtils.isNotBlank(selector) ? selector.substring(1) : "properties";
    }
}
