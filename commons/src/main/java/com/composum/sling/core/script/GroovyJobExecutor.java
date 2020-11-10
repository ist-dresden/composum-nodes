package com.composum.sling.core.script;

import com.composum.sling.core.concurrent.AbstractJobExecutor;
import com.composum.sling.core.concurrent.SequencerService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Callable;

import static com.composum.sling.core.script.GroovyRunner.DEFAULT_SETUP_SCRIPT;
import static com.composum.sling.core.util.ResourceUtil.*;

@Component(
        service = {JobExecutor.class, EventHandler.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Groovy Job Executor Service",
                JobExecutor.PROPERTY_TOPICS + "=" + GroovyJobExecutor.GROOVY_TOPIC,
                EventConstants.EVENT_TOPIC + "=" + "org/apache/sling/event/notification/job/*"
        },
        immediate = true
)
@Designate(ocd = GroovyJobExecutor.Configuration.class)
public class GroovyJobExecutor extends AbstractJobExecutor<Object> {

    @ObjectClassDefinition(name = "Composum Groovy Job Executor Service",
            description = "Provides the execution of groovy scripts in the repository context.")
    public @interface Configuration {
        @AttributeDefinition(name = "Groovy setup script",
                description = "the optional path to a custom groovy script to setup a groovy runner script object")
        String groovy_setup_script() default DEFAULT_SETUP_SCRIPT;
    }

    private static final Logger LOG = LoggerFactory.getLogger(GroovyJobExecutor.class);
    static final String GROOVY_TOPIC = "com/composum/sling/core/script/GroovyJobExecutor";
    private static final String SCRIPT_PROPERTY_NAME = "reference";

    private static final String AUDIT_BASE_PATH = AUDIT_ROOT_PATH + "com.composum.sling.core.script.GroovyJobExecutor";

    private volatile String groovySetupScript;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SequencerService<SequencerService.Token> sequencer;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Nonnull
    protected ResourceResolverFactory getResolverFactory() {
        return resolverFactory;
    }

    @Nonnull
    protected SequencerService<SequencerService.Token> getSequencer() {
        return sequencer;
    }

    @Nonnull
    protected DynamicClassLoaderManager getDynamicClassLoaderManager() {
        return dynamicClassLoaderManager;
    }

    @Activate @Modified
    protected void activate(Configuration configuration) {
        groovySetupScript = StringUtils.defaultIfBlank(configuration.groovy_setup_script(), DEFAULT_SETUP_SCRIPT);
    }

    @Deactivate
    protected void deactivate() {
        groovySetupScript = null;
    }

    @Nonnull
    @Override
    protected String getJobTopic() {
        return GROOVY_TOPIC;
    }

    @Override
    @Nonnull
    protected String getAuditBasePath() {
        return AUDIT_BASE_PATH;
    }

    @Override
    protected boolean jobExecutionEnabled(Job job) {
        // if -Dcomposum.never.start.groovy=true is set on command line, no scripts will be executed.
        return !Boolean.getBoolean("composum.never.start.groovy");
    }

    @Override
    protected String buildAuditPathIntern(String reference, Calendar eventJobStartedTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        if (reference.endsWith("/jcr:content")) {
            return getAuditBasePath() + reference.substring(0, reference.lastIndexOf('/')) + "/" + sdf.format(eventJobStartedTime.getTime());
        } else {
            return getAuditBasePath() + reference + "/" + sdf.format(eventJobStartedTime.getTime());
        }
    }

    @Override
    protected Callable<Object> createCallable(final Job job, final JobExecutionContext context,
                                              final ResourceResolver serviceResolver, final PrintWriter out)
            throws Exception {
        return new GroovyRunnerCallable(job, context, serviceResolver, out);
    }

    @Override
    protected void jobExecutionFinished(Job job, JobExecutionContext context, Resource auditResource) throws IOException {
        final String scriptPath = job.getProperty(JOB_REFRENCE_PROPERTY, String.class);
        final ResourceResolver resourceResolver = auditResource.getResourceResolver();
        final Resource scriptResource = resourceResolver.getResource(scriptPath);
        final String scriptname;
        if (scriptPath.endsWith("/jcr:content")) {
            final String substringWithoutContent = scriptPath.substring(0, scriptPath.lastIndexOf('/'));
            scriptname = substringWithoutContent.substring(substringWithoutContent.lastIndexOf('/') + 1);
        } else {
            scriptname = scriptPath.substring(scriptPath.lastIndexOf('/') + 1);
        }
        Resource scriptAuditResource = resourceResolver.create(auditResource, scriptname, new HashMap<String, Object>() {{
            put(PROP_PRIMARY_TYPE, TYPE_FILE);
        }});
        try (final InputStream inputStream = scriptResource.adaptTo(InputStream.class);) {
            resourceResolver.create(scriptAuditResource, CONTENT_NODE, new HashMap<String, Object>() {{
                put(PROP_PRIMARY_TYPE, TYPE_RESOURCE);
                put(PROP_MIME_TYPE, "text/x-groovy");
                put(PROP_DATA, inputStream);
            }});
        }
        resourceResolver.commit();
    }

    protected class GroovyRunnerCallable extends UserContextCallable {

        GroovyRunnerCallable(final Job job, final JobExecutionContext context,
                             final ResourceResolver serviceResolver, final PrintWriter out)
                throws RepositoryException, LoginException {
            super(job, context, serviceResolver, out);
        }

        @Override
        public Object call() throws Exception {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(dynamicClassLoaderManager.getDynamicClassLoader());
            final GroovyRunner groovyRunner = new GroovyRunner(session, out, groovySetupScript);
            final HashMap<String, Object> variables = new HashMap<>();
            variables.put("jctx", context);
            variables.put("job", job);
            final String reference = job.getProperty(JOB_REFRENCE_PROPERTY, String.class);
            final String script;
            if (reference.endsWith("/jcr:content")) {
                script = reference.substring(0, reference.lastIndexOf('/'));
            } else {
                script = reference;
            }
            try {
                return groovyRunner.run(script, variables);
            } finally {
                Thread.currentThread().setContextClassLoader(tccl);
                close();
            }
        }
    }
}
