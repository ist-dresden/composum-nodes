package com.composum.sling.core.script;

import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Service(value = {JobExecutor.class, EventHandler.class})
@Properties({
        @Property(
                name = JobExecutor.PROPERTY_TOPICS,
                value = GroovyJobExecutor.GROOVY_TOPIC,
                propertyPrivate = true),
        @Property(
                name = EventConstants.EVENT_TOPIC,
                value = {"org/apache/sling/event/notification/job/*"},
                propertyPrivate = true)
})
public class GroovyJobExecutor implements JobExecutor, EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyJobExecutor.class);
    private static final Map<String, Object> CRUD_CACHE_FOLDER_PROPS;
    static final String GROOVY_TOPIC = "com/composum/sling/core/script/GroovyJobExecutor";

    static {
        Map<String, Object> map = new HashMap<>();
        map.put(ResourceUtil.PROP_PRIMARY_TYPE, "sling:Folder");
        CRUD_CACHE_FOLDER_PROPS = Collections.unmodifiableMap(map);
    }

    private static final String AUDIT_BASE_PATH = "/var/audit/jobs/com.composum.sling.core.script.GroovyJobExecutor";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    protected SequencerService<SequencerService.Token> sequencer;


    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        String userId = job.getProperty("userid", String.class);
        String outfile = job.getProperty("outfile", String.class);
        final String script = job.getProperty("script", String.class);
        final String auditPath = buildAuditPath(job);
        ResourceResolver adminResolver;
        File tempFile = new File(outfile);
        try {
            adminResolver = resolverFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            return context.result().message(e.getMessage()).failed();
        }
        Session adminSession = adminResolver.adaptTo(Session.class);
        Resource auditResource = giveParent(adminResolver, auditPath);
        try {
            Session session = adminSession.impersonate(new SimpleCredentials(userId, new char[0]));
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
                 PrintWriter out = new PrintWriter(fileOutputStream)) {
                final GroovyRunner groovyRunner = new GroovyRunner(session, out);
                final HashMap<String, Object> variables = new HashMap<>();
                variables.put("jctx", context);
                variables.put("job", job);
                final ExecutorService executorService = Executors.newSingleThreadExecutor();
                final Future<Object> submit = executorService.submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return groovyRunner.run(script, variables);
                    }
                });
                while (!submit.isDone()) {
                    Thread.yield();
                    out.flush();
                    if (context.isStopped()) {
                        LOG.warn("context for script {} stopped", script);
                        executorService.shutdownNow();
                        // magic string. message must not be changed!
                        return context.result().message("execution stopped").cancelled();
                    }
                }
                final Object run = submit.get();
                if (run != null) {
                    context.log(run.toString());
                }
            }
            return context.result().message(tempFile.getPath()).succeeded();
        } catch (Exception e) {
            LOG.error("Error executing groovy script Job.", e);
            return context.result().message(e.getMessage()).failed();
        } finally {
            try {
                Resource logFileResource = adminResolver.create(auditResource, outfile.substring(outfile.lastIndexOf(File.separator)+1), new HashMap<String, Object>() {{
                    put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_FILE);
                }});
                try (final InputStream inputStream = new FileInputStream(tempFile)) {
                    adminResolver.create(logFileResource, ResourceUtil.CONTENT_NODE, new HashMap<String, Object>() {{
                        put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_RESOURCE);
                        put(ResourceUtil.PROP_MIME_TYPE, "text/plain");
                        put(ResourceUtil.PROP_DATA, inputStream);
                    }});
                }
                final Set<String> propertyNames = job.getPropertyNames();
                final ModifiableValueMap map = auditResource.adaptTo(ModifiableValueMap.class);
                for (String propertyName: propertyNames) {
                    if (!propertyName.startsWith("jcr:") && !propertyName.equals("sling:resourceType")) {
                        final Object property = job.getProperty(propertyName);
                        map.put(propertyName, property);
                    }
                }
                adminResolver.commit();
            } catch (Exception e) {
                LOG.error("Error writing audit of groovy script Job.", e);
            }
            adminResolver.close();
        }
    }

    private String buildAuditPath(Job job) {
        String script = job.getProperty("script", String.class);
        final Calendar eventJobStartetTime = job.getProperty("event.job.started.time", Calendar.class);
        return buildAuditPathIntern(script, eventJobStartetTime);
    }

    private String buildAuditPathIntern(String script, Calendar eventJobStartetTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        return AUDIT_BASE_PATH + script + "/" + sdf.format(eventJobStartetTime.getTime());
    }

    private synchronized Resource giveParent(ResourceResolver resolver, String path) {
        Resource resource = null;
        SequencerService.Token token = sequencer.acquire(path);
        try {
            resource = resolver.getResource(path);
            if (resource == null) {
                String[] separated = splitPathAndName(path);
                Resource parent = giveParent(resolver, separated[0]);
                try {
                    resource = resolver.create(parent, separated[1], CRUD_CACHE_FOLDER_PROPS);
                    resolver.commit();
                } catch (PersistenceException pex) {
                    // catch it and hope that the parent is available
                    // necessary to continue on transaction isolation problems
                    LOG.error("GroovyJobExecutor giveParent('" + path + "'): " + pex.toString());
                }
            }
        } finally {
            sequencer.release(token);
        }
        return resource;
    }

    private static String[] splitPathAndName(String path) {
        String[] result = new String[2];
        int nameSeparator = path.lastIndexOf('/');
        result[0] = path.substring(0, nameSeparator);
        result[1] = path.substring(nameSeparator + 1);
        return result;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(NotificationConstants.TOPIC_JOB_FINISHED) ||
                event.getTopic().equals(NotificationConstants.TOPIC_JOB_FAILED) ||
                event.getTopic().equals(NotificationConstants.TOPIC_JOB_CANCELLED)) {
            final String topic = (String)event.getProperty(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC);
            if (topic.equals(GROOVY_TOPIC)) {
                final String script = (String) event.getProperty("script");
                final Calendar eventJobStartetTime = (Calendar) event.getProperty("event.job.started.time");
                final String auditPath = buildAuditPathIntern(script, eventJobStartetTime);
                ResourceResolver adminResolver = null;
                try {
                    adminResolver = resolverFactory.getAdministrativeResourceResolver(null);
                    final Resource auditResource = adminResolver.getResource(auditPath);
                    final ModifiableValueMap map = auditResource.adaptTo(ModifiableValueMap.class);
                    if (event.containsProperty("slingevent:resultMessage")) {
                        map.put("slingevent:resultMessage", event.getProperty("slingevent:resultMessage"));
                    }
                    if (event.containsProperty("slingevent:finishedDate")) {
                        map.put("slingevent:finishedDate", event.getProperty("slingevent:finishedDate"));
                    } else {
                        //FIXME slingevent:finishedDate is never part of the job properties
                        map.put("slingevent:finishedDate", GregorianCalendar.getInstance());
                    }
                    if (event.containsProperty("slingevent:finishedState")) {
                        map.put("slingevent:finishedState", event.getProperty("slingevent:finishedState"));
                    } else {
                        //FIXME slingevent:finishedState is never part of the job properties
                        switch (event.getTopic()) {
                            case NotificationConstants.TOPIC_JOB_FINISHED:
                                map.put("slingevent:finishedState", Job.JobState.SUCCEEDED.name());
                                break;
                            case NotificationConstants.TOPIC_JOB_CANCELLED:
                                if ("execution stopped".equals(event.getProperty("slingevent:resultMessage"))) {
                                    map.put("slingevent:finishedState", Job.JobState.STOPPED.name());
                                } else {
                                    map.put("slingevent:finishedState", Job.JobState.ERROR.name());
                                }
                                break;
                            case NotificationConstants.TOPIC_JOB_FAILED:
                                map.put("slingevent:finishedState", Job.JobState.GIVEN_UP.name());
                                break;
                        }
                    }
                    adminResolver.commit();
                } catch (LoginException | PersistenceException e) {
                    LOG.error("Error extending audit log of groovy script Job.", e);
                } finally {
                    if (adminResolver != null) {
                        adminResolver.close();
                    }
                }
            }
        }
    }
}
