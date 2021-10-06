package com.composum.sling.nodes.browser;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.StringFilter;
import com.composum.sling.core.util.I18N;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.nodes.components.codeeditor.CodeEditorServlet;
import com.composum.sling.nodes.console.ConsoleServletBean;
import com.composum.sling.nodes.scene.SceneConfigurations;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.composum.sling.core.util.CoreConstants.PROP_RESOURCE_SUPER_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

public class Browser extends ConsoleServletBean {

    private static final Logger LOG = getLogger(Browser.class);

    public static final String TYPE_FILE = "nt:file";
    public static final String TYPE_RESOURCE = "nt:resource";
    public static final String OAK_RESOURCE = "oak:Resource";
    public static final String TYPE_UNSTRUCTURED = "nt:unstructured";

    public static final String HTML = "html";
    public static final String JSP = "jsp";
    public static final String PDF = "pdf";

    public static final String PROP_DATA = "jcr:data";
    public static final String PROP_MIME_TYPE = "jcr:mimeType";
    public static final String DEFAULT_MIME_TYPE = "text/html";

    public static final Map<String, String> EDITOR_MODES;

    static {
        EDITOR_MODES = new HashMap<>();
        EDITOR_MODES.put("config", "text");
        EDITOR_MODES.put("css", "css");
        EDITOR_MODES.put("dart", "dart");
        EDITOR_MODES.put("ecma", "javascript");
        EDITOR_MODES.put("esp", "jsp");
        EDITOR_MODES.put("groovy", "groovy");
        EDITOR_MODES.put(HTML, HTML);
        EDITOR_MODES.put("java", "java");
        EDITOR_MODES.put("javascript", "javascript");
        EDITOR_MODES.put("js", "javascript");
        EDITOR_MODES.put("json", "json");
        EDITOR_MODES.put(JSP, JSP);
        EDITOR_MODES.put("less", "less");
        EDITOR_MODES.put("markdown", "markdown");
        EDITOR_MODES.put("md", "markdown");
        EDITOR_MODES.put("scala", "scala");
        EDITOR_MODES.put("text", "text");
        EDITOR_MODES.put("txt", "text");
        EDITOR_MODES.put("xml", "xml");
        EDITOR_MODES.put("xslt", "xml");
        EDITOR_MODES.put("xslt+xml", "xml");
    }

    public static final Map<String, String> FILE_ICONS;

    static {
        FILE_ICONS = new HashMap<>();
        FILE_ICONS.put(PDF, PDF);
        FILE_ICONS.put("jar", "archive");
        FILE_ICONS.put("far", "archive");
        FILE_ICONS.put("war", "archive");
        FILE_ICONS.put("zip", "archive");
        FILE_ICONS.put("tar", "archive");
        FILE_ICONS.put("gtar", "archive");
        FILE_ICONS.put("gzip", "archive");
        FILE_ICONS.put("gz", "archive");
        FILE_ICONS.put("audio", "audio");
        FILE_ICONS.put("au", "audio");
        FILE_ICONS.put("snd", "audio");
        FILE_ICONS.put("wav", "audio");
        FILE_ICONS.put("aif", "audio");
        FILE_ICONS.put("aiff", "audio");
        FILE_ICONS.put("aifc", "audio");
        FILE_ICONS.put("ram", "audio");
        FILE_ICONS.put("ra", "audio");
        FILE_ICONS.put("mp2", "audio");
        FILE_ICONS.put("msword", "word");
        FILE_ICONS.put("doc", "word");
        FILE_ICONS.put("docx", "word");
        FILE_ICONS.put("msexcel", "excel");
        FILE_ICONS.put("xls", "excel");
        FILE_ICONS.put("xlsx", "excel");
        FILE_ICONS.put("mspowerpoint", "powerpoint");
        FILE_ICONS.put("ppt", "powerpoint");
        FILE_ICONS.put("pptx", "powerpoint");
    }

    private transient MergeMountpointService mergeMountpointService;

    public static class Reference {

        @Nonnull
        protected final String label;
        @Nullable
        protected final String tooltip;
        @Nonnull
        protected final String path;
        @Nullable
        protected final String actions;

        public Reference(@Nonnull final String label, @Nullable final String tooltip, @Nonnull final String path) {
            this(label, tooltip, path, null);
        }

        public Reference(@Nonnull final String label, @Nullable final String tooltip, @Nonnull final String path,
                         @Nullable final String actions) {
            this.label = label;
            this.tooltip = tooltip;
            this.path = path;
            this.actions = actions;
        }

        @Nonnull
        public String getLabel() {
            return label;
        }

        @Nullable
        public String getTooltip() {
            return tooltip;
        }

        @Nonnull
        public String getPath() {
            return path;
        }

        @Nonnull
        public String getActions() {
            return actions != null ? actions : "";
        }

        @Override
        public String toString() {
            return "Reference{" + "label='" + label + '\'' +
                    ", actions='" + actions + '\'' +
                    ", path='" + path + '\'' +
                    '}';
        }
    }

    private transient String primaryType;
    private transient String resourceType;

    private transient Boolean isDeclaringType;
    private transient List<String> supertypeChain;
    private transient Map<String, Reference> resourceTypes;
    private transient Map<String, Reference> relatedPathSet;

    private transient Boolean overlayAvailable;
    private transient Boolean overrideAvailable;
    private transient Map<String, Reference> typeRootLabels;

    private transient String mimeType;
    private transient String nameExtension;

    private transient String viewType;
    private transient String textType;
    private transient String fileIcon;

    private transient Boolean isRenderable;

    private transient Boolean isFile;
    private transient Boolean isAsset;
    private transient Boolean isImage;
    private transient Boolean isVideo;
    private transient Boolean isText;

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
                    primaryType = getResource().getValueMap()
                            .get(JcrConstants.JCR_PRIMARYTYPE, "{no node}");
                }
            } catch (RepositoryException ex) {
                primaryType = getResource().getValueMap()
                        .get(JcrConstants.JCR_PRIMARYTYPE, String.format("{%s}", ex.getMessage()));
            }
        }
        return primaryType;
    }

    //
    // Node/Resource - Content Type
    //

    /**
     * returns 'true' if the current resource has a well known resource type
     */
    public boolean isTyped() {
        return StringUtils.isNotBlank(getResourceType());
    }

    /**
     * @return 'true' if the current resource itself declares a resource type
     */
    public boolean isDeclaringType() {
        if (isDeclaringType == null) {
            isDeclaringType = false;
            String path = getPath();
            if (path.startsWith(getOverrideRoot() + "/")) {
                path = path.substring(getOverrideRoot().length());
            }
            for (String root : getTypeSearchPath(true)) {
                if (path.startsWith(root)) {
                    isDeclaringType = true;
                    break;
                }
            }
        }
        return isDeclaringType;
    }

    /**
     * @return 'true' if the current resource itself 'implements' a resource type
     */
    public boolean isSourcePath() {
        return isDeclaringType() && !isOverlayResource() && !isOverrideResource();
    }

    /**
     * the content resource type (sling:resourceType) declared for the current resource
     */
    @Nonnull
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
                if (!Resource.RESOURCE_TYPE_NON_EXISTING.equals(type)
                        && !getPath().equals(getOverlayRoot() + "/" + type)) {
                    resourceType = type;
                }
            }
        }
        return resourceType;
    }

    /**
     * Remove any search path / /mnt/overlay from given path to a resource type = "normalize" path to resource type.
     */
    @Nullable
    protected String getResourceType(@Nullable String resourceType) {
        if (StringUtils.isNotBlank(resourceType)) {
            if (resourceType.startsWith(getOverrideRoot())) {
                resourceType = resourceType.substring(getOverrideRoot().length());
            }
            for (String root : getTypeSearchPath(true)) {
                if (resourceType.startsWith(root)) {
                    resourceType = resourceType.substring(root.length());
                    break;
                }
            }
        }
        return resourceType;
    }

    /**
     * Returns the resource for a resourceType (the "highest" in the search path), or the resource if the path is absolute.
     */
    @Nullable
    protected Resource getTypeResource(@Nullable final String resourceType, boolean includeOverlay) {
        ResourceResolver resolver = getResolver();
        if (StringUtils.isNotBlank(resourceType)) {
            if (!resourceType.startsWith("/")) {
                for (String root : getTypeSearchPath(includeOverlay)) {
                    Resource typeResource = resolver.getResource(root + resourceType);
                    if (typeResource != null) {
                        return typeResource;
                    }
                }
            }
            return resolver.getResource(resourceType);
        }
        return null;
    }

    @Nonnull
    protected List<String> getTypeSearchPath(boolean includeOverlay) {
        List<String> typeSearchPath = new ArrayList<>(Arrays.asList(getResolver().getSearchPath()));
        if (includeOverlay) {
            typeSearchPath.add(0, getOverlayRoot() + "/");
        }
        return typeSearchPath;
    }

    /**
     * The chain of resource super types. This is also included for content resources since this is used quite often in AEM.
     *
     * @see "https://experienceleague.adobe.com/docs/experience-manager-65/developing/introduction/the-basics.html?lang=en#sling-request-processing"
     */
    @Nonnull
    public List<String> getSupertypeChain() {
        if (supertypeChain == null) {
            supertypeChain = new ArrayList<>();
            Resource typeResource = resource;
            if (isDeclaringType()) { // start from "highest" resource wrt. search path
                typeResource = getTypeResource(getResourceType(getPath()), false);
            }
            while (typeResource != null) {
                ValueMap values = typeResource.getValueMap();
                typeResource = getTypeResource(values.get(PROP_RESOURCE_SUPER_TYPE, ""), false);
                if (typeResource != null) {
                    supertypeChain.add(typeResource.getPath());
                }
            }
        }
        return supertypeChain;
    }

    /**
     * Paths for the locations relevant to the resource typein search paths, /mnt/override / /mnt/overlay, mapped to the label information.
     */
    @Nonnull
    protected Map<String, Reference> getResourceTypeSet() {
        if (resourceTypes == null) {
            resourceTypes = new LinkedHashMap<>();
            String resourceType = getResourceType(isDeclaringType() ? getPath() : getResourceType());
            if (StringUtils.isNotBlank(resourceType)) {
                ResourceResolver resolver = getResolver();
                Map<String, Reference> labels = getTypeRootLabels();
                if (isOverrideAvailable()) {
                    Reference label = labels.get(getOverrideRoot() + "/");
                    String path = getOverridePath();
                    resourceTypes.put(label.getLabel(), new Reference(label.getLabel(),
                            label.getTooltip() + "\n" + path, path));
                }
                if (isOverlayAvailable()) {
                    Reference label = labels.get(getOverlayRoot() + "/");
                    String path = getOverlayPath();
                    resourceTypes.put(label.getLabel(), new Reference(label.getLabel(),
                            label.getTooltip() + "\n" + path, path));
                }
                String basePath = getBasePath();
                for (String root : getTypeSearchPath(false)) {
                    String resourceTypePath = root + resourceType;
                    Resource type = resolver.getResource(resourceTypePath);
                    Reference label = labels.get(root); // XXX
                    resourceTypes.put(label.getLabel(), new Reference(label.getLabel(),
                            label.getTooltip() + "\n" + resourceTypePath, resourceTypePath, type != null ?
                            (basePath != null && resolver.getResource(basePath) != null ? "is-overlay" : null)
                            : "overlay-option"));
                }
            }
        }
        return resourceTypes;
    }

    /**
     * Set of related paths: for resource types the resource type found in the search path and /mnt/(override|overlay), base paths, resource types.
     */
    @Nonnull
    public Map<String, Reference> getRelatedPathSet() {
        if (relatedPathSet == null) {
            if (isDeclaringType()) {
                relatedPathSet = getResourceTypeSet();
            } else {
                relatedPathSet = new LinkedHashMap<>();
                Map<String, Reference> labels = getTypeRootLabels();
                String overrideRoot = getOverrideRoot() + "/";
                if (isOverrideAvailable()) {
                    Reference label = labels.get(overrideRoot);
                    String overridePath = getOverridePath();
                    String basePath = getBasePath();
                    relatedPathSet.put(label.getLabel(), new Reference(label.getLabel(),
                            label.getTooltip() + "\n" + overridePath, overridePath));
                    relatedPathSet.put("B", new Reference("B",
                            I18N.get(getRequest(), "Original (Base Path)") + "\n" + basePath, basePath));
                }
                String resourceType = getResourceType();
                if (StringUtils.isNotBlank(resourceType)) {
                    Resource type = getTypeResource(resourceType, false);
                    if (type != null) {
                        String typePath = type.getPath();
                        relatedPathSet.put("T", new Reference("T",
                                I18N.get(getRequest(), "Resource Type") + "\n" + typePath, typePath));
                    }
                }
            }
        }
        return relatedPathSet;
    }

    //
    // resource merger overlay / override
    //

    public String getBasePath() {
        if (isOverrideResource()) {
            return getPath().substring(getOverrideRoot().length());
        } else if (isOverlayResource()) { // use "highest" found entry according to search path
            Resource type = getTypeResource(getResourceType(isDeclaringType() ? getPath() : getResourceType()), false);
            return type != null ? type.getPath() : getPath();
        }
        return getPath();
    }

    public String getOverlayRoot() {
        return getMergeMountpointService().overlayMergeMountPoint(getResolver());
    }

    private MergeMountpointService getMergeMountpointService() {
        if (mergeMountpointService == null) {
            mergeMountpointService = this.getSling().getService(MergeMountpointService.class);
        }
        return mergeMountpointService;
    }

    public boolean isOverlayResource() {
        return getPath().startsWith(getOverlayRoot() + "/");
    }

    public boolean isOverlayAvailable() {
        if (overlayAvailable == null) {
            String overlayPath = getOverlayPath();
            overlayAvailable = overlayPath != null && getResolver().getResource(overlayPath) != null;
        }
        return overlayAvailable;
    }

    /**
     * Path of resource type within /mnt/overlay . If not a declaring resource, this doesn't make sense -> null.
     */
    @Nullable
    public String getOverlayPath() {
        return isOverlayResource() ? getPath() :
                isDeclaringType() ? getOverlayRoot() + "/" + getResourceType(getPath())
                        : null;
    }

    public String getOverrideRoot() {
        return getMergeMountpointService().overrideMergeMountPoint(getResolver());
    }

    public boolean isOverrideResource() {
        return getPath().startsWith(getOverrideRoot() + "/");
    }

    public boolean isOverrideAvailable() {
        if (overrideAvailable == null) {
            overrideAvailable = getResolver().getResource(getOverridePath()) != null;
        }
        return overrideAvailable;
    }

    /**
     * Path within /mnt/override.
     */
    @Nonnull
    public String getOverridePath() {
        return isOverrideResource() ? getPath() : getOverrideRoot() + getBasePath();
    }

    protected Map<String, Reference> getTypeRootLabels() {
        if (typeRootLabels == null) {
            typeRootLabels = new HashMap<>();
            typeRootLabels.put(getOverrideRoot() + "/",
                    new Reference("o/r", "Resource Merger - Override", getOverrideRoot()));
            typeRootLabels.put(getOverlayRoot() + "/",
                    new Reference("o/l", "Resource Merger - Overlay", getOverlayRoot()));
            for (String root : getResolver().getSearchPath()) {
                String label = ("" + root.charAt(1)).toUpperCase();
                String path = StringUtils.removeEnd(root, "/");
                typeRootLabels.put(root, new Reference(label, "Resource Resolver - " + path, path));
            }
        }
        return typeRootLabels;
    }

    //
    // Component Scenes
    //

    public boolean isSceneAvailable() {
        Collection<SceneConfigurations.Config> availableScenes = getAvailableScenes();
        return !availableScenes.isEmpty();
    }

    @Nonnull
    public Collection<SceneConfigurations.Config> getAvailableScenes() {
        return SceneConfigurations.instance(getRequest()).getSceneConfigs();
    }

    //
    // File type
    //

    public boolean isRenderable() {
        if (isRenderable == null) {
            String extension = getNameExtension();
            isRenderable = isTyped()
                    || (isText() && (HTML.equals(extension) /*|| JSP.equals(extension)*/))
                    || (isFile() && (PDF.equals(extension)));
        }
        return isRenderable;
    }

    public boolean isFile() {
        if (isFile == null) {
            isFile = false;
            ResourceHandle contentResource = getContentResource();
            if (contentResource == null) {
                contentResource = resource; // use node itself if no content present (only in the Browser!)
            }
            if (contentResource != null) {
                ValueMap values = contentResource.getValueMap();
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
        return isFile;
    }

    @Nonnull
    public String getFilePath() {
        if (isFile()) {
            ResourceHandle fileRes = resource;
            if (JcrConstants.JCR_CONTENT.equals(resource.getName())) {
                fileRes = resource.getParent();
            }
            if (fileRes != null && fileRes.isOfType(TYPE_FILE)) {
                return fileRes.getPath();
            }
        }
        return "";
    }

    @Nonnull
    public String getFileIcon() {
        if (fileIcon == null) {
            String mimeType = getMimeType();
            String extension = getNameExtension();
            String icon = getFileType(FILE_ICONS, mimeType, extension);
            fileIcon = StringUtils.isNotBlank(icon) ? "file-" + icon + "-o" : "file-o";
        }
        return fileIcon;
    }

    public InputStream openFile() {
        if (isFile()) {
            ResourceHandle contentResource = getContentResource();
            if (contentResource == null) {
                contentResource = resource; // use node itself if no content present
            }
            if (contentResource != null) {
                ValueMap values = contentResource.getValueMap();
                return values.get(JcrConstants.JCR_DATA, InputStream.class);
            }
        }
        return null;
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

    public boolean isJcrResource() {
        return !ResourceUtil.isSyntheticResource(resource) && resource.adaptTo(Node.class) != null;
    }

    public boolean isCanHaveAcl() {
        return isJcrResource();
    }

    public boolean isVersionable() {
        if (!isJcrResource()) {
            return false;
        }
        try {
            final VersionManager versionManager = getSession().getWorkspace().getVersionManager();
            versionManager.getBaseVersion(getPath());
            return true;
        } catch (UnsupportedRepositoryOperationException e) {
            return false; // OK - node is simply not versionable.
        } catch (RepositoryException e) {
            LOG.error("Bug: unknown error on " + getPath(), e); // shouldn't happen - please check why.
            return false;
        }
    }

    public static final Pattern SETUP_SCRIPT_PATTERN =
            Pattern.compile(".*[\"']?acl[\"']?\\s*:\\s*[\\[{].*", Pattern.MULTILINE | Pattern.DOTALL);

    private transient Boolean setupScript;

    public boolean isSetupScript() {
        if (setupScript == null) {
            setupScript = false;
            if (isText() && "json".equals(getTextType())) {
                setupScript = SETUP_SCRIPT_PATTERN.matcher(getTextSnippet()).matches();
            }
        }
        return setupScript;
    }

    private transient String textSnippet;

    @Nonnull
    public String getTextSnippet() {
        if (textSnippet == null) {
            textSnippet = "";
            if (isText()) {
                try (InputStream data = openFile()) {
                    if (data != null) {
                        char[] buffer = new char[512];
                        int len = IOUtils.read(new InputStreamReader(data), buffer);
                        textSnippet = new String(buffer, 0, len);
                    }
                } catch (IOException ex) {
                    LOG.error(ex.toString());
                }
            }
        }
        return textSnippet;
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
            textType = getFileType(EDITOR_MODES, mimeType, extension);
        }
        return textType;
    }

    /**
     * Determines the text type for the current node using the mimeType (if present) and the extension.
     *
     * @return the type of the text file (script language) or ""
     */
    public static String getFileType(Map<String, String> typeMap, String mimeType, String extension) {
        String textType = null;
        if (StringUtils.isNotBlank(mimeType)) {
            textType = typeMap.get(mimeType);
            if (StringUtils.isBlank(textType)) {
                String[] parts = StringUtils.split(mimeType, '/');
                if (parts.length > 1) {
                    textType = typeMap.get(parts[1]);
                }
                if (StringUtils.isBlank(textType)) {
                    if (StringUtils.isNotBlank(extension)) {
                        textType = typeMap.get(extension);
                    }
                    if (StringUtils.isBlank(textType)) {
                        textType = typeMap.get(parts[0]);
                    }
                }
            }
        }
        if (StringUtils.isBlank(textType)) {
            if (StringUtils.isNotBlank(extension)) {
                textType = typeMap.get(extension);
            }
        }
        if (textType == null) {
            textType = "";
        }
        return textType;
    }

    public String getViewType() {
        if (viewType == null) {
            BrowserViews.View genericView = BrowserViews.getView(context, getResource());
            if (genericView != null) {
                return "generic";
            }
            viewType = "something";
            if (isFile()) {
                if (isVideo()) {
                    viewType = "video";
                } else if (isImage()) {
                    viewType = "image";
                } else if (isText()) {
                    if ("groovy".equalsIgnoreCase(getTextType())/* || isSetupScript() TODO?*/) {
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
                    } else {
                        if (isDeclaringType()) {
                            viewType = "component";
                        }
                    }
                }
            }
        }
        return viewType;
    }

    public String getTabType() {
        String tabType = getRequest().getSelectors(new StringFilter.BlackList("^tab$"));
        if (StringUtils.isBlank(tabType)) {
            tabType = "properties";
        } else if (tabType.startsWith(".generic.")) {
            tabType = "generic";
        } else {
            tabType = tabType.substring(1);
        }
        return tabType;
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
