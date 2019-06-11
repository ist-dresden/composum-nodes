package com.composum.sling.core.concurrent;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.composum.sling.core.util.ResourceUtil.CONTENT_NODE;
import static com.composum.sling.core.util.ResourceUtil.PROP_DATA;
import static com.composum.sling.core.util.ResourceUtil.PROP_MIME_TYPE;
import static com.composum.sling.core.util.ResourceUtil.PROP_PRIMARY_TYPE;
import static com.composum.sling.core.util.ResourceUtil.PROP_RESOURCE_TYPE;
import static com.composum.sling.core.util.ResourceUtil.TYPE_FILE;
import static com.composum.sling.core.util.ResourceUtil.TYPE_RESOURCE;
import static com.composum.sling.core.util.ResourceUtil.splitPathAndName;
import static org.apache.sling.event.jobs.Job.JobState.ERROR;
import static org.apache.sling.event.jobs.Job.JobState.GIVEN_UP;
import static org.apache.sling.event.jobs.Job.JobState.STOPPED;
import static org.apache.sling.event.jobs.Job.JobState.SUCCEEDED;
import static org.apache.sling.event.jobs.Job.PROPERTY_FINISHED_DATE;
import static org.apache.sling.event.jobs.Job.PROPERTY_JOB_STARTED_TIME;
import static org.apache.sling.event.jobs.Job.PROPERTY_RESULT_MESSAGE;
import static org.apache.sling.event.jobs.NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC;
import static org.apache.sling.event.jobs.NotificationConstants.TOPIC_JOB_CANCELLED;
import static org.apache.sling.event.jobs.NotificationConstants.TOPIC_JOB_FAILED;
import static org.apache.sling.event.jobs.NotificationConstants.TOPIC_JOB_FINISHED;

@Component(componentAbstract = true)
public abstract class AbstractJobExecutor<Result> implements JobExecutor, EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJobExecutor.class);

    private final Lock lock = new ReentrantLock(true);

    public static final String JOB_REFRENCE_PROPERTY = "reference";
    public static final String JOB_OUTFILE_PROPERTY = "outfile";
    public static final String JOB_USERID_PROPERTY = "userid";

    public static final String AUDIT_ROOT_PATH = "/var/audit/jobs/";

    public static final Map<String, Object> CRUD_AUDIT_FOLDER_PROPS;

    static {
        Map<String, Object> map = new HashMap<>();
        map.put(PROP_PRIMARY_TYPE, "sling:Folder");
        CRUD_AUDIT_FOLDER_PROPS = Collections.unmodifiableMap(map);
    }

    @Reference
    protected ResourceResolverFactory resolverFactory;

    @Reference
    protected SequencerService<SequencerService.Token> sequencer;

    @Reference
    protected DynamicClassLoaderManager dynamicClassLoaderManager;

    @Activate
    protected void activate(ComponentContext context) throws Exception {
        Dictionary<String, Object> properties = context.getProperties();
    }

    protected abstract String getJobTopic();

    protected abstract String getAuditBasePath();

    protected abstract boolean jobExecutionEnabled(Job job);

    /**
     * The Callable does the job which is prepared by the executor.
     */
    protected abstract Callable<Result> createCallable(final Job job,
                                                       final JobExecutionContext context,
                                                       final ResourceResolver adminResolver,
                                                       final PrintWriter out)
            throws Exception;

    protected abstract class UserContextCallable implements Callable<Result> {

        protected final Job job;
        protected final JobExecutionContext context;
        protected final ResourceResolver serviceResolver;
        protected final PrintWriter out;
        protected final Session session;

        public UserContextCallable(final Job job, final JobExecutionContext context,
                                   final ResourceResolver serviceResolver, final PrintWriter out)
                throws RepositoryException, LoginException {
            this.job = job;
            this.context = context;
            this.serviceResolver = serviceResolver;
            this.out = out;
            String userId = job.getProperty(JOB_USERID_PROPERTY, String.class);
            Session serviceSession = serviceResolver.adaptTo(Session.class);
            session = serviceSession.impersonate(new SimpleCredentials(userId, new char[0]));
            HashMap<String, Object> authInfo = new HashMap<>();
            authInfo.put("user.jcr.session", session);
        }

        public void close() {
            session.logout();
        }
    }

    @Override
    public final JobExecutionResult process(final Job job, final JobExecutionContext context) {

        final String reference = job.getProperty(JOB_REFRENCE_PROPERTY, String.class);

        if (!jobExecutionEnabled(job)) {
            return context.result().message("job execution disabled, job:" + reference + " cancelled.").cancelled();
        }

        String outfile = job.getProperty(JOB_OUTFILE_PROPERTY, String.class);
        final String auditPath = buildAuditPath(job);

        ResourceResolver adminResolver;
        File tempFile = new File(outfile);
        try {
            adminResolver = resolverFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            return context.result().message(e.getMessage()).cancelled();
        }

        Resource auditResource;
        lock.lock();
        try {
            try {
                adminResolver.adaptTo(Session.class).refresh(true);
                auditResource = giveParent(adminResolver, auditPath);
                adminResolver.commit();
            } catch (PersistenceException|RepositoryException e) {
                LOG.error("Error creating audit of Job.", e);
                adminResolver.close();
                return context.result().message(e.getMessage()).cancelled();
            }
        } finally {
            lock.unlock();
        }

        try {
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(fileOutputStream, "UTF-8"))) {
                final ExecutorService executorService = Executors.newSingleThreadExecutor();
                try {
                    final Future<Result> submit = executorService.submit(
                            createCallable(job, context, adminResolver, out));
                    while (!submit.isDone()) {
                        Thread.yield();
                        out.flush();
                        if (context.isStopped()) {
                            LOG.warn("context for job:{} stopped", reference);
                            executorService.shutdownNow();
                            // magic string. message must not be changed!
                            return context.result().message("execution stopped").cancelled();
                        }
                    }
                    final Object run = submit.get();
                    return context.result().message(String.valueOf(run)).succeeded();
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof JobFailureException) {
                        LOG.error("Error executing job:" + reference, cause);
                        return context.result().message(cause.getMessage()).cancelled();
                    } else {
                        LOG.error("Error executing job:" + reference, e);
                        e.printStackTrace(out);
                        return context.result().message(e.getMessage()).cancelled();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error executing job:" + reference, e);
            return context.result().message(e.toString()).cancelled();
        } finally {
            try {
                Resource logFileResource = adminResolver.create(auditResource, outfile.substring(outfile.lastIndexOf(File.separator) + 1), new HashMap<String, Object>() {{
                    put(PROP_PRIMARY_TYPE, TYPE_FILE);
                }});
                try (final InputStream inputStream = new FileInputStream(tempFile)) {
                    adminResolver.create(logFileResource, CONTENT_NODE, new HashMap<String, Object>() {{
                        put(PROP_PRIMARY_TYPE, TYPE_RESOURCE);
                        put(PROP_MIME_TYPE, "text/plain");
                        put(PROP_DATA, inputStream);
                    }});
                }
                final boolean deleted = tempFile.delete();
                final Set<String> propertyNames = job.getPropertyNames();
                final ModifiableValueMap map = auditResource.adaptTo(ModifiableValueMap.class);
                for (String propertyName : propertyNames) {
                    if (!propertyName.startsWith("jcr:") && !propertyName.equals(PROP_RESOURCE_TYPE)) {
                        final Object property = job.getProperty(propertyName);
                        map.put(propertyName, property);
                    }
                }
                map.put(PROP_RESOURCE_TYPE, "composum/nodes/jobcontrol/audit");
                adminResolver.commit();
                jobExecutionFinished(job, context, auditResource);
            } catch (Exception e) {
                LOG.error("Error writing audit of job:" + reference, e);
            }
            adminResolver.close();
        }
    }

    /**
     * Can be overwritten to handle some cleanup or logging when job is finished.
     */
    protected void jobExecutionFinished(Job job, JobExecutionContext context, Resource auditResource) throws IOException {

    }

    private String buildAuditPath(Job job) {
        String reference = job.getProperty(JOB_REFRENCE_PROPERTY, String.class);
        final Calendar eventJobStartedTime = job.getProperty("event.job.started.time", Calendar.class);
        return buildAuditPathIntern(reference, eventJobStartedTime);
    }

    protected String buildAuditPathIntern(String reference, Calendar eventJobStartedTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        return getAuditBasePath() + reference + "/" + sdf.format(eventJobStartedTime.getTime());
    }

    private synchronized Resource giveParent(ResourceResolver resolver, String path) {
        if ("/".equals(path))
            return resolver.getResource("/");

        Resource resource = null;
        SequencerService.Token token = sequencer.acquire(path);
        try {
            resource = resolver.getResource(path);
            if (resource == null) {
                String[] separated = splitPathAndName(path);
                Resource parent = giveParent(resolver, separated[0]);
                try {
                    resource = resolver.create(parent, separated[1], CRUD_AUDIT_FOLDER_PROPS);
                    resolver.commit();
                } catch (PersistenceException pex) {
                    // catch it and hope that the parent is available
                    // necessary to continue on transaction isolation problems
                    LOG.error("AbstractJobExecutor giveParent('" + path + "'): " + pex.toString());
                }
            }
        } finally {
            sequencer.release(token);
        }
        return resource;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(TOPIC_JOB_FINISHED) ||
                event.getTopic().equals(TOPIC_JOB_FAILED) ||
                event.getTopic().equals(TOPIC_JOB_CANCELLED)) {
            final String topic = (String) event.getProperty(NOTIFICATION_PROPERTY_JOB_TOPIC);
            if (topic.equals(getJobTopic())) {
                final String script = (String) event.getProperty(JOB_REFRENCE_PROPERTY);
                final Calendar eventJobStartedTime = (Calendar) event.getProperty(PROPERTY_JOB_STARTED_TIME);
                final String auditPath = buildAuditPathIntern(script, eventJobStartedTime);
                ResourceResolver adminResolver = null;
                try {
                    adminResolver = resolverFactory.getAdministrativeResourceResolver(null);
                    final Resource auditResource = adminResolver.getResource(auditPath);
                    final ModifiableValueMap map = auditResource.adaptTo(ModifiableValueMap.class);
                    if (event.containsProperty(PROPERTY_RESULT_MESSAGE)) {
                        map.put(PROPERTY_RESULT_MESSAGE, event.getProperty(PROPERTY_RESULT_MESSAGE));
                    }
                    if (event.containsProperty(PROPERTY_FINISHED_DATE)) {
                        final Calendar finishedDate = (Calendar) event.getProperty(PROPERTY_FINISHED_DATE);
                        map.put(PROPERTY_FINISHED_DATE, finishedDate);
                        final long executionTime = finishedDate.getTimeInMillis() - eventJobStartedTime.getTimeInMillis();
                        map.put("executionTime", executionTime);
                    } else {
                        //FIXME slingevent:finishedDate is never part of the job properties
                        final Calendar finishedDate = GregorianCalendar.getInstance();
                        map.put(PROPERTY_FINISHED_DATE, finishedDate);
                        final long executionTime = finishedDate.getTimeInMillis() - eventJobStartedTime.getTimeInMillis();
                        map.put("executionTime", executionTime);
                    }
                    if (event.containsProperty("slingevent:finishedState")) {
                        map.put("slingevent:finishedState", event.getProperty("slingevent:finishedState"));
                    } else {
                        //FIXME slingevent:finishedState is never part of the job properties
                        switch (event.getTopic()) {
                            case TOPIC_JOB_FINISHED:
                                map.put("slingevent:finishedState", SUCCEEDED.name());
                                break;
                            case TOPIC_JOB_CANCELLED:
                                if ("execution stopped".equals(event.getProperty("slingevent:resultMessage"))) {
                                    map.put("slingevent:finishedState", STOPPED.name());
                                } else {
                                    map.put("slingevent:finishedState", ERROR.name());
                                }
                                break;
                            case TOPIC_JOB_FAILED:
                                map.put("slingevent:finishedState", GIVEN_UP.name());
                                break;
                        }
                    }
                    adminResolver.commit();
                } catch (LoginException | PersistenceException e) {
                    LOG.error("Error extending audit log of job execution", e);
                } finally {
                    if (adminResolver != null) {
                        adminResolver.close();
                    }
                }
            }
        }
    }

    // helpers

    protected <T> T getProperty(Job job, String name, T defaultValue) {
        Object value = job.getProperty(name);
        return value != null ? (T) value : defaultValue;
    }
}
