package com.composum.sling.core.event;

import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * the abstract observer implementation to react on property changes
 */
public abstract class AbstractChangeObserver implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractChangeObserver.class);

    public static final int EVENT_TYPES = Event.NODE_ADDED |
            Event.NODE_REMOVED |
            Event.NODE_MOVED |
            Event.PROPERTY_ADDED |
            Event.PROPERTY_CHANGED |
            Event.PROPERTY_REMOVED;

    public static final String PROP_LAST_MODIFIED_BY = "jcr:lastModifiedBy";

    public static final String LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * ignore 'jcr:..' properties except: 'jcr:title', 'jcr:description'
     */
    public static final StringFilter PROPERTY_PATH_FILTER =
            new StringFilter.FilterSet(StringFilter.FilterSet.Rule.or,
                    new StringFilter.BlackList("/(jcr|sling):[^/]*$"),
                    new StringFilter.WhiteList("/jcr:(title|description|data)[^/]*$"));

    protected BundleContext bundleContext;

    // to complete for a change observer...

    /**
     * returns the user id used by the observer (used to detect self generated events)
     */
    protected abstract String getServiceUserId();

    /**
     * determines the root path (probably configured) for the observer registration
     */
    protected abstract String getObservedPath();

    /**
     * performs the change for the handler implementation
     */
    protected abstract void doOnChange(ResourceResolver resolver, ChangedResource change)
            throws RepositoryException, PersistenceException;

    /**
     * returns 'true' if the node is the target node for the change
     */
    protected abstract boolean isTargetNode(Node node) throws RepositoryException;

    /**
     * returns 'null' if the target node traversal should ends (extension hook)
     */
    protected String getTargetPath(Node node) throws RepositoryException {
        String path = node.getPath();
        return "/".equals(path) ? null : path;
    }

    /**
     * extension hook to determine the handler filter based on the events property path
     */
    protected StringFilter getPropertyPathFilter() {
        return PROPERTY_PATH_FILTER;
    }

    /**
     * extension hook to determine the handler filter based on the events node path
     */
    protected StringFilter getNodePathFilter() {
        return StringFilter.ALL;
    }

    /**
     * extension hook to determine the handler filter based on the found target resource
     */
    protected ResourceFilter getResourceFilter() {
        return ResourceFilter.ALL;
    }

    /**
     * performs the right login and returns the resolver
     */
    protected abstract ResourceResolver getResolver() throws LoginException;

    /**
     * performs the right login and returns the session
     */
    protected abstract Session getSession() throws RepositoryException;

    // event handler...

    /**
     * the collection to register all changed target resources of the event list
     */
    protected class ChangeCollection extends HashMap<String, ChangedResource> {

        public void registerChange(Session session, ResourceResolver resolver, String path, Calendar time, String user)
                throws RepositoryException {
            Node contentNode = getContentNode(session, path);
            if (contentNode != null) {
                path = contentNode.getPath();
                ChangedResource change = get(path);
                if (change != null) {
                    change.mergeChange(time, user);
                } else {
                    if (LOG.isDebugEnabled()) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat(LOG_DATE_FORMAT);
                        LOG.debug("registered: " + path + ", " + dateFormat.format(time.getTime()) + ", " + user);
                    }
                    Resource resource = resolver.getResource(path);
                    if (resource != null && getResourceFilter().accept(resource)) {
                        put(path, new ChangedResource(resource, time, user));
                    }
                }
            }
        }
    }

    /**
     * the collection item for a change to perform
     */
    protected class ChangedResource {

        protected final Resource resource;
        protected Calendar time;
        protected String user;

        public ChangedResource(Resource resource, Calendar time, String user) {
            this.resource = resource;
            this.time = time;
            this.user = user;
        }

        public void mergeChange(Calendar time, String user) {
            if (time.after(this.time)) {
                this.time = time;
                this.user = user;
            }
        }

        public Resource getResource() {
            return resource;
        }

        public Calendar getTime() {
            return time;
        }

        public String getUser() {
            return user;
        }
    }

    /**
     * collects the changed nodes and calls the observers strategy (doOnChange) for each node found
     */
    @Override
    public void onEvent(EventIterator events) {
        try {
            // this resolver should be the only one (for this handling thread)
            ResourceResolver resolver = getResolver();
            if (resolver != null) {
                try {
                    Session session = resolver.adaptTo(Session.class);
                    String serviceUserId = getServiceUserId();
                    // collect changed nodes
                    ChangeCollection changedNodes = new ChangeCollection();
                    while (events.hasNext()) {
                        Event event = events.nextEvent();
                        if (ignoreEvent(event)) continue;
                        try {
                            String path = event.getPath();
                            String user = event.getUserID();
                            // if the service user is the initiator this is a self initiated event - ignore it
                            if (!serviceUserId.equals(user)) {
                                Calendar time = Calendar.getInstance();
                                time.setTime(new Date(event.getDate()));
                                int type = event.getType();
                                if (isPropertyEvent(type)) {
                                    if (getPropertyPathFilter().accept(path)) {
                                        changedNodes.registerChange(session, resolver, path, time, user);
                                    }
                                } else {
                                    if (getNodePathFilter().accept(path)) {
                                        changedNodes.registerChange(session, resolver, path, time, user);
                                    }
                                }
                            }
                        } catch (RepositoryException rex) {
                            LOG.error(rex.getMessage(), rex);
                        }
                    }
                    // handle change actions on the detected nodes
                    if (changedNodes.size() > 0) {
                        for (ChangedResource change : changedNodes.values()) {
                            try {
                                doOnChange(resolver, change);
                            } catch (RepositoryException ex) {
                                LOG.error(ex.getMessage(), ex);
                            }
                        }
                        changedNodes.clear();
                        resolver.commit();
                    }
                } catch (PersistenceException ex) {
                    LOG.error(ex.getMessage(), ex);
                } finally {
                    resolver.close();
                }
            }
        } catch (LoginException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    /** We avoid processing external events since we can't access all data on these and we avoid duplicated processing. */
    protected boolean ignoreEvent(Event event) {
        return (event instanceof JackrabbitEvent) && ((JackrabbitEvent) event).isExternal();
    }

    /**
     * determines the target node (the node to perform the change) of one event item
     */
    protected Node getContentNode(Session session, String path)
            throws RepositoryException {
        Node node = null;
        try {
            Item item = session.getItem(path);
            if (item.isNode()) {
                node = (Node) item;
            } else {
                node = item.getParent();
            }
            while (node != null
                    && !isTargetNode(node)
                    && (path = getTargetPath(node)) != null) {
                node = node.getParent();
            }
        } catch (PathNotFoundException ignore) {
            // probably removed... ignore
        }
        return path != null ? node : null;
    }

    /**
     * returns 'trus' if the event is of type 'property change'
     */
    protected boolean isPropertyEvent(int type) {
        return (type & (Event.PROPERTY_ADDED |
                Event.PROPERTY_CHANGED |
                Event.PROPERTY_REMOVED)) != 0;
    }

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
        try {
            Session session = getSession();
            session.getWorkspace().getObservationManager().addEventListener(
                    this, EVENT_TYPES, getObservedPath(),
                    true, null, null, true);
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Deactivate
    protected void deactivate() {
        try {
            Session session = getSession();
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
