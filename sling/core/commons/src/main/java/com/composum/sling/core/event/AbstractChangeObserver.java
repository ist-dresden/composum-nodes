package com.composum.sling.core.event;

import com.composum.sling.core.filter.StringFilter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
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

    /**
     * the collection to register all changed nodes of the event list
     */
    protected class ChangeCollection extends HashMap<String, ChangedContent> {

        public void registerChange(Session session, String path, Calendar time, String user)
                throws RepositoryException {
            Node contentNode = getContentNode(session, path);
            if (contentNode != null) {
                path = contentNode.getPath();
                ChangedContent change = get(path);
                if (change != null) {
                    change.mergeChange(time, user);
                } else {
                    if (LOG.isDebugEnabled()) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat(LOG_DATE_FORMAT);
                        LOG.debug("registered: " + path + ", " + dateFormat.format(time.getTime()) + ", " + user);
                    }
                    put(path, new ChangedContent(contentNode, time, user));
                }
            }
        }
    }

    protected class ChangedContent {

        protected final Node node;
        protected Calendar time;
        protected String user;

        public ChangedContent(Node node, Calendar time, String user) {
            this.node = node;
            this.time = time;
            this.user = user;
        }

        public void mergeChange(Calendar time, String user) {
            if (time.after(this.time)) {
                this.time = time;
                this.user = user;
            }
        }

        public Node getNode() {
            return node;
        }

        public Calendar getTime() {
            return time;
        }

        public String getUser() {
            return user;
        }
    }

    protected abstract String getServiceUserId();

    protected abstract String getObservedPath();

    protected abstract void doOnChange(ResourceResolver resolver, ChangedContent change)
            throws RepositoryException, PersistenceException;

    protected abstract boolean isContentNode(Node node) throws RepositoryException;

    protected abstract ResourceResolver getResolver() throws LoginException;

    protected abstract Session getSession() throws RepositoryException;


    /**
     * collects the changed nodes and calls the observers strategy (doOnChange) for each node found
     */
    @Override
    public void onEvent(EventIterator events) {
        try {
            ResourceResolver resolver = getResolver();
            if (resolver != null) {
                try {
                    Session session = resolver.adaptTo(Session.class);
                    String serviceUserId = getServiceUserId();
                    // collect changed nodes
                    ChangeCollection changedNodes = new ChangeCollection();
                    while (events.hasNext()) {
                        Event event = events.nextEvent();
                        try {
                            String path = event.getPath();
                            String user = event.getUserID();
                            // if the service user is the initiator this is a self initiated event - ignore it
                            if (!serviceUserId.equals(user)) {
                                Calendar time = Calendar.getInstance();
                                time.setTime(new Date(event.getDate()));
                                int type = event.getType();
                                if (isPropertyEvent(type)) {
                                    if (PROPERTY_PATH_FILTER.accept(path)) {
                                        changedNodes.registerChange(session, path, time, user);
                                    }
                                } else {
                                    changedNodes.registerChange(session, path, time, user);
                                }
                            }
                        } catch (RepositoryException rex) {
                            LOG.error(rex.getMessage(), rex);
                        }
                    }
                    // handle change actions on the detected nodes
                    if (changedNodes.size() > 0) {
                        for (ChangedContent change : changedNodes.values()) {
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

    /**
     * determine the abstract 'content node' of one item
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
                    && !isContentNode(node)
                    && !"/".equals(path = node.getPath())) {
                node = node.getParent();
            }
        } catch (PathNotFoundException ex) {
            // probably removed... ignore
        }
        return !"/".equals(path) ? node : null;
    }

    protected boolean isPropertyEvent(int type) {
        return (type & (Event.PROPERTY_ADDED |
                Event.PROPERTY_CHANGED |
                Event.PROPERTY_REMOVED)) != 0;
    }

    protected void activate() {
        try {
            Session session = getSession();
            session.getWorkspace().getObservationManager().addEventListener(
                    this, EVENT_TYPES, getObservedPath(),
                    true, null, null, true);
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    protected void deactivate() {
        try {
            Session session = getSession();
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}