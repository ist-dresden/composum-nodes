package com.composum.sling.nodes.browser;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.RequestHandle;
import com.composum.sling.core.Restricted;
import com.composum.sling.core.util.LinkMapper;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.nodes.console.ConsoleServletBean;
import com.composum.sling.nodes.servlet.NodeServlet;
import org.apache.commons.lang3.StringUtils;
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
    @NotNull
    public String getId() {
        return browserView.getId();
    }

    public String getMappedUrl() {
        if (mappedUrl == null) {
            RequestHandle request = getRequest();
            mappedUrl = LinkUtil.getUrl(request, getPath(), null, "", LinkMapper.RESOLVER);
        }
        return mappedUrl;

    }

    public String getUnmappedUrl() {
        if (unmappedUrl == null) {
            RequestHandle request = getRequest();
            unmappedUrl = LinkUtil.getUrl(request, getPath(), null, "", LinkMapper.CONTEXT);
        }
        return unmappedUrl;
    }

    @NotNull
    public String getViewResourceType() {
        final String resourceType = browserView.getViewResourceType();
        return StringUtils.isNotBlank(resourceType) ? resourceType : "composum/nodes/browser/view/generic";
    }

    @NotNull
    public String getTabResourceType() {
        return browserView.getTabResourceType();
    }

    @NotNull
    public List<BrowserViews.View.Tab> getViewTabs() {
        if (viewTabs == null) {
            viewTabs = browserView.getTabs(context, getResource());
        }
        return viewTabs;
    }

    @NotNull
    public BrowserViews.View.Toolbar getToolbar() {
        if (viewToolbar == null) {
            viewToolbar = browserView.getToolbar(context, getResource());
        }
        return viewToolbar;
    }

    @NotNull
    public BrowserViews.View.Content getContent() {
        if (viewContent == null) {
            viewContent = browserView.getContent(context, getResource());
        }
        return viewContent;
    }

    @NotNull
    public String getMimeTypeCss() {
        final String mimeType = getMimeType();
        return StringUtils.isNotBlank(mimeType)
                ? mimeType.substring(mimeType.indexOf('/') + 1).replaceAll("[+]", " ")
                : "";
    }

    /**
     * the content mime type declared for the current resource
     */
    public String getMimeType() {
        MimeType mimeType = MimeTypeUtil.getMimeType(getResource());
        return mimeType != null ? mimeType.toString() : "";
    }
}
