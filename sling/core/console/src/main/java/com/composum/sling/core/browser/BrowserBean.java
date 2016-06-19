package com.composum.sling.core.browser;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.console.ConsoleServletBean;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.MimeTypeUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BrowserBean extends ConsoleServletBean {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserBean.class);

    public static final String DEFAULT_MIME_TYPE = "text/html";

    private transient NodeHandle current;
    private transient NodeHandle parent;
    private transient List<NodeHandle> parents;

    public BrowserBean(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public BrowserBean(BeanContext context) {
        super(context);
    }

    public BrowserBean() {
        super();
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
                    name =  "jcr:root";
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
                pathUrl += LinkUtil.getExtension(resource, "");
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
                url = LinkUtil.getUrl(getRequest(), getPath());
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
            parents = new ArrayList<NodeHandle>();
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
