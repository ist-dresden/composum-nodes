package com.composum.sling.nodes.browser;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.RequestHandle;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.util.LinkMapper;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.nodes.console.ConsoleServletBean;
import com.composum.sling.nodes.servlet.NodeServlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.tika.mime.MimeType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Restricted(key = NodeServlet.SERVICE_KEY)
public class GenericView extends ConsoleServletBean {

    private static final Logger LOG = getLogger(GenericView.class);

    protected BrowserViews.View browserView;

    private transient List<BrowserViews.View.Tab> viewTabs;
    private transient BrowserViews.View.Toolbar viewToolbar;
    private transient BrowserViews.View.Content viewContent;

    private transient String mappedUrl;
    private transient String unmappedUrl;

    private transient String viewType;
    private transient String fileType;
    private transient String mimeType;
    private transient Boolean isRenderable;
    private transient ResourceHandle fileResource;

    public GenericView(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public GenericView(BeanContext context) {
        super(context);
    }

    public GenericView() {
        super();
    }

    @Override
    public void initialize(BeanContext context, Resource resource) {
        super.initialize(context, resource);
        browserView = BrowserViews.getView(context, resource);
    }

    @Override
    public @NotNull String getId() {
        return browserView.getId();
    }

    public @NotNull String getMappedUrl() {
        if (mappedUrl == null) {
            RequestHandle request = getRequest();
            mappedUrl = LinkUtil.getUrl(request, getPath(), null, "", LinkMapper.RESOLVER);
        }
        return mappedUrl;

    }

    public @NotNull String getUnmappedUrl() {
        if (unmappedUrl == null) {
            RequestHandle request = getRequest();
            unmappedUrl = LinkUtil.getUrl(request, getPath(), null, "", LinkMapper.CONTEXT);
        }
        return unmappedUrl;
    }

    public @NotNull String getViewResourceType() {
        final String resourceType = browserView.getViewResourceType();
        return StringUtils.isNotBlank(resourceType) ? resourceType : "composum/nodes/browser/view/generic";
    }

    public @NotNull String getTabResourceType() {
        return browserView.getTabResourceType();
    }

    public @NotNull List<BrowserViews.View.Tab> getViewTabs() {
        if (viewTabs == null) {
            viewTabs = browserView.getTabs(context, getResource());
        }
        return viewTabs;
    }

    public @NotNull BrowserViews.View.Toolbar getToolbar() {
        if (viewToolbar == null) {
            viewToolbar = browserView.getToolbar(context, getResource());
        }
        return viewToolbar;
    }

    public @NotNull BrowserViews.View.Content getContent() {
        if (viewContent == null) {
            viewContent = browserView.getContent(context, getResource());
        }
        return viewContent;
    }

    public @NotNull String getViewType() {
        if (viewType == null) {
            viewType = isImage() ? "image" : isVideo() ? "video" : Browser.isFile(getResource()) ? "file" : "something";
        }
        return viewType;
    }

    public @NotNull String getFileType() {
        if (fileType == null) {
            StringBuilder type = new StringBuilder();
            if (Browser.isFile(getResource())) {
                type.append("file-").append(StringUtils.substringBefore(getMimeType(), "/"));
                final String extension = ResourceUtil.getNameExtension(getResource());
                if (StringUtils.isNotBlank(extension)) {
                    type.append("-").append(extension);
                }
            }
            fileType = type.toString();
        }
        return fileType;
    }

    public @NotNull ResourceHandle getFileResource() {
        if (fileResource == null) {
            ResourceHandle resource = getResource();
            Resource original = resource.getChild(JcrConstants.JCR_CONTENT + "/renditions/original");
            fileResource = original != null ? ResourceHandle.use(original) : resource;
        }
        return fileResource;
    }

    public boolean isRenderable() {
        if (isRenderable == null) {
            isRenderable = Browser.isRenderable(getFileResource(), ResourceUtil.getNameExtension(getResource()));
        }
        return isRenderable;
    }

    public @NotNull String getFileIcon() {
        return Browser.getFileIcon(getFileResource());
    }

    public @NotNull String getMimeTypeCss() {
        final String mimeType = getMimeType();
        return StringUtils.isNotBlank(mimeType)
                ? mimeType.substring(mimeType.indexOf('/') + 1).replaceAll("[+]", " ")
                : "";
    }

    /**
     * the content mime type declared for the current resource
     */
    public @NotNull String getMimeType() {
        if (mimeType == null) {
            MimeType mType = MimeTypeUtil.getMimeType(getFileResource());
            mimeType = mType != null ? mType.toString() : "";
        }
        return mimeType;
    }

    /**
     * 'true' if the mimetype is of type image
     */
    public boolean isImage() {
        return getMimeType().startsWith("image/");
    }

    /**
     * 'true' if the mimetype is of type video
     */
    public boolean isVideo() {
        return getMimeType().startsWith("video/");
    }
}
