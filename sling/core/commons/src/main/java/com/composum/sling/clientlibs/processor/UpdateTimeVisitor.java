package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.*;
import com.composum.sling.clientlibs.service.ClientlibService;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedHashSet;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Determines the hash of the embedded items of a clientlib or -category for recency checking of cached files.
 */
public class UpdateTimeVisitor extends AbstractClientlibVisitor {

    private static final Logger LOG = getLogger(UpdateTimeVisitor.class);

    protected Calendar lastUpdateTime = null;

    public UpdateTimeVisitor(ClientlibElement owner, ClientlibService service, ResourceResolver resolver) {
        this(owner, service, resolver, null);
    }

    protected UpdateTimeVisitor(ClientlibElement owner, ClientlibService service, ResourceResolver resolver,
                                LinkedHashSet<ClientlibLink> processedElements) {
        super(owner, service, resolver, processedElements);
    }

    @Override
    public UpdateTimeVisitor execute() throws IOException, RepositoryException {
        super.execute();
        return this;
    }

    /**
     * Returns the last update time estimated from the file update times and clientlib folder create times.
     * This is only a lower bound for the update time of the client library including its embedded files.
     */
    public Calendar getLastUpdateTime() {
        if (null != lastUpdateTime && lastUpdateTime.after(Calendar.getInstance())) {
            LOG.warn("Last update time newer than now {} for {}", lastUpdateTime, owner);
            return null; // Something's broken - we don't know
        }
        return lastUpdateTime;
    }

    @Override
    protected ClientlibVisitor createVisitorFor(ClientlibElement element) {
        return new UpdateTimeVisitor(element, service, resolver, processedElements);
    }

    @Override
    public void action(Clientlib clientlib, VisitorMode mode, ClientlibResourceFolder parent) {
        updateTime(clientlib, clientlib.resource.getLastModified());
    }

    @Override
    public void action(ClientlibFile file, VisitorMode mode, ClientlibResourceFolder parent) {
        if (VisitorMode.EMBEDDED == mode) {
            Calendar lastModified = file.handle.getLastModified();
            updateTime(file, lastModified);
        }
    }

    protected void updateTime(Object entity, Calendar resourceTime) {
        if (LOG.isDebugEnabled())
            LOG.debug("{} modification on {}", entity, null != resourceTime ? resourceTime.getTime() : "null");
        if (null == lastUpdateTime || (null != resourceTime && resourceTime.after(lastUpdateTime)))
            lastUpdateTime = resourceTime;
    }

}
