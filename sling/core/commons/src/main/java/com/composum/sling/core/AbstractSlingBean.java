package com.composum.sling.core;

import com.composum.sling.core.request.DomIdentifiers;
import com.composum.sling.core.util.LinkUtil;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * The abstract base class for 'Beans' to implement a Model based on e JCR resource without a mapping framework.
 * Such a 'bean' can be declared as variable in aJSP context using the 'component' tag of the Composum 'nodes'
 * tag library (cpnl).
 */
public abstract class AbstractSlingBean implements SlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSlingBean.class);

    // pre filled attributes (filled during initialization)

    /** the instance of the scripting context for the bean (initialized) */
    protected BeanContext context;

    /** the resource represented by this bean (initialized) */
    protected ResourceHandle resource;

    // all transient attributes are not pre filled during initialization
    // - a getter must be used to access to this members.
    // The scope is set to 'protected' to enable pre filling in subclasses.

    protected transient SlingScriptHelper sling;
    protected transient ResourceResolver resolver;
    protected transient Session session;
    protected transient QueryManager queryManager;
    protected transient RequestHandle request;
    protected transient SlingHttpServletResponse response;

    // the property or structure caching attributes (initialized in their getter method)

    private transient String name;
    private transient String path;
    private transient String type;
    private transient String domId;
    private transient String id;
    private transient String url;

    private transient String title;

    private transient Node node;

    /**
     * initialize bean using the context an the resource given explicitly
     */
    public AbstractSlingBean(BeanContext context, Resource resource) {
        initialize(context, resource);
    }

    /**
     * initialize bean using the context with the 'resource' attribute within
     */
    public AbstractSlingBean(BeanContext context) {
        initialize(context);
    }

    /**
     * if this constructor is used, the bean must be initialized using the 'initialize' method!
     */
    public AbstractSlingBean() {
    }

    /**
     * Uses the contexts 'resource' attribute for initialization (content.getResource()).
     *
     * @param context the scripting context (e.g. a JSP PageContext or a Groovy scripting context)
     */
    public void initialize(BeanContext context) {
        initialize(context, context.getResource());
    }

    /**
     * This basic initialization sets up the context and resource attributes only,
     * all the other attributes are set 'lazy' during their getter calls.
     *
     * @param context  the scripting context (e.g. a JSP PageContext or a Groovy scripting context)
     * @param resource the resource to use (normally the resource addressed by the request)
     */
    public void initialize(BeanContext context, Resource resource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("initialize (" + context + ", " + resource + ")");
        }
        this.context = context;
        this.resource = ResourceHandle.use(resource);
    }

    /**
     * Returns the handle to the 'Sling world' and all available services.
     */
    public SlingScriptHelper getSling() {
        if (sling == null) {
            sling = context.getAttribute("sling", SlingScriptHelper.class);
        }
        return sling;
    }

    /**
     * Returns the resolver using the resource of this bean (resource.getResolver()).
     */
    public ResourceResolver getResolver() {
        if (resolver == null) {
            resolver = getResource().getResourceResolver();
        }
        return resolver;
    }

    /**
     * the getter for the resource which defines this bean instance.
     */
    public ResourceHandle getResource() {
        return resource;
    }

    /**
     * Returns the URL to the resource of this bean (mapped and with the appropriate extension).
     *
     * @see LinkUtil#getUrl(SlingHttpServletRequest, String)
     */
    public String getUrl() {
        if (url == null) {
            RequestHandle request = getRequest();
            url = LinkUtil.getUrl(request, getPath(), request.getSelectors(), null);
        }
        return url;
    }

    public String getPath() {
        if (path == null) {
            path = getResource().getPath();
        }
        return path;
    }

    public String getName() {
        if (name == null) {
            name = getResource().getName();
        }
        return name;
    }

    public String getType() {
        if (type == null) {
            type = getResource().getResourceType();
        }
        return type;
    }

    public String getDomId() {
        if (domId == null) {
            domId = DomIdentifiers.getInstance(context).getElementId(this);
        }
        return domId;
    }

    public String getId() {
        if (id == null) {
            id = getResource().getId();
        }
        return id;
    }

    public Node getNode() {
        if (node == null) {
            node = getResource().getNode();
            if (node == null) {
                node = context.getAttribute("currentNode", Node.class);
            }
        }
        return node;
    }

    /**
     * Determine a typed parent resource.
     */
    public ResourceHandle getParent(String resourceType) {
        ResourceHandle result = getResource();
        while (result != null && result.isValid() && !result.isResourceType(resourceType)) {
            result = result.getParent();
        }
        if (result != null && !result.isValid()) {
            result = getParent(resourceType, getPath()); // implicit fallback to the path
        }
        return result;
    }

    /**
     * Use path instead of resource (e.g. if resource is synthetic or non existing) to determine a typed parent.
     */
    public ResourceHandle getParent(String resourceType, String path) {
        ResourceResolver resolver = getResolver();
        Resource resource;
        while (((resource = resolver.getResource(path)) == null || !resource.isResourceType(resourceType))
                && StringUtils.isNotBlank(path)) {
            int delimiter = path.lastIndexOf('/');
            if (delimiter >= 0) {
                path = path.substring(0, delimiter);
            } else {
                break;
            }
        }
        return ResourceHandle.use(resource);
    }

    // Properties

    public boolean getHasTitle() {
        return StringUtils.isNotBlank(getTitle());
    }

    public String getTitle() {
        if (title == null) {
            title = getProperty("title", "");
            if (StringUtils.isBlank(title)) {
                title = getProperty(ResourceUtil.PROP_TITLE, "");
            }
        }
        return title;
    }

    public ResourceHandle getContentResource() {
        return resource.getContentResource();
    }

    public <T> T getProperty(String key, T defaultValue) {
        return resource.getContentProperty(key, defaultValue);
    }

    public <T> T getProperty(String key, Class<T> type) {
        return resource.getContentProperty(key, type);
    }

    public <T> T getInherited(String key, T defaultValue) {
        return resource.getInherited(key, defaultValue);
    }

    public <T> T getInherited(String key, Class<T> type) {
        return resource.getInherited(key, type);
    }

    //

    public RequestHandle getRequest() {
        if (request == null) {
            SlingHttpServletRequest req = context.getRequest();
            if (req != null){
                request = RequestHandle.use(req);
            }
        }
        return request;
    }

    public SlingHttpServletResponse getResponse() {
        if (response == null) {
            response = context.getResponse();
        }
        return response;
    }

    //
    // JCR Query helpers
    //

    public interface NodeClosure {

        void call(Node node) throws RepositoryException;
    }

    public void executeQuery(String queryString, NodeClosure closure) throws RepositoryException {
        NodeIterator iterator = findNodes(queryString);
        while (iterator.hasNext()) {
            closure.call(iterator.nextNode());
        }
    }

    public <T extends AbstractSlingBean> List<T> findBeans(String queryString, Class<T> type) {
        List<T> result = new ArrayList<>();
        try {
            Constructor<T> constructor = type.getConstructor(BeanContext.class, Resource.class);
            NodeIterator iterator = findNodes(queryString);
            ResourceResolver resolver = getResolver();
            Resource resource;
            Node node;
            while (iterator.hasNext()) {
                node = iterator.nextNode();
                resource = resolver.getResource(node.getPath());
                if (resource != null) {
                    result.add(constructor.newInstance(context, resource));
                }
            }
        } catch (Exception rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return result;
    }

    public List<String> findPathList(String queryString) throws RepositoryException {
        List<String> result = new ArrayList<>();
        NodeIterator iterator = findNodes(queryString);
        while (iterator.hasNext()) {
            result.add(iterator.nextNode().getPath());
        }
        return result;
    }

    public NodeIterator findNodes(String queryString) throws RepositoryException {
        //noinspection deprecation
        return findNodes(queryString, Query.XPATH);
    }

    public NodeIterator findNodes(String queryString, String type) throws RepositoryException {
        Query query = getQueryManager().createQuery(queryString, type);
        QueryResult result = query.execute();
        return result.getNodes();
    }

    public QueryManager getQueryManager() throws RepositoryException {
        if (queryManager == null) {
            queryManager = getSession().getWorkspace().getQueryManager();
        }
        return queryManager;
    }

    //
    // JCR Session helpers
    //

    public Session getSession() {
        if (session == null) {
            session = getResolver().adaptTo(Session.class);
        }
        return session;
    }

    public String getUsername() {
        Principal principal = getRequest().getUserPrincipal();
        return principal != null ? principal.getName() : "";
    }

    /**
     * A 'toString' implementation for logging and debugging.
     *
     * @param builder the buffer to write into
     */
    public void toString(StringBuilder builder) {
        String jvmId = getStringId();
        String id = getId();
        if (id.equals(jvmId)) {
            builder.append(jvmId);
        } else {
            jvmId = jvmId.substring(jvmId.indexOf('@' + 1));
            builder.append(getClass().getSimpleName());
            builder.append('(').append(id).append('/').append(jvmId).append(')');
        }
        builder.append(",resource:").append(resource);
    }

    /**
     * Returns the default 'toString' value with the JVM 'id' of the object.
     *
     * @return the general JVM 'id'
     */
    public String getStringId() {
        return super.toString();
    }

    /**
     * Default implementation: uses {@link #toString(StringBuilder)}.
     *
     * @return a string representation for debugging.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
}
