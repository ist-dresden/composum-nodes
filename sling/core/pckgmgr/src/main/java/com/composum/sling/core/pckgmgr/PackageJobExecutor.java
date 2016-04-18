package com.composum.sling.core.pckgmgr;

import com.composum.sling.core.concurrent.AbstractJobExecutor;
import com.composum.sling.core.pckgmgr.util.PackageProgressTracker;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.concurrent.Callable;

import static com.composum.sling.core.pckgmgr.util.PackageUtil.IMPORT_DONE;

@Component(
        label = "Package Job Executor Service",
        description = "Provides the execution of package operations the repository context.",
        immediate = true,
        metatype = true
)
@Service(value = {JobExecutor.class, EventHandler.class})
@Properties({
        @Property(
                name = JobExecutor.PROPERTY_TOPICS,
                value = PackageJobExecutor.TOPIC,
                propertyPrivate = true),
        @Property(
                name = EventConstants.EVENT_TOPIC,
                value = {"org/apache/sling/event/notification/job/*"},
                propertyPrivate = true)
})
public class PackageJobExecutor extends AbstractJobExecutor<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(PackageJobExecutor.class);

    public static final String JOB_PROPERTY_DRY_RUN = "dryRun";
    public static final String JOB_PROPERTY_SAVE_THRESHOLD = "saveThreshold";
    public static final String JOB_PROPERTY_IMPORT_MODE = "importMode";

    public static final String TOPIC = "com/composum/sling/core/pckgmgr/PackageJobExecutor";

    public static final String AUDIT_BASE_PATH = AUDIT_ROOT_PATH + PackageJobExecutor.class.getName();

    public static final String DEFAULT_SAVE_THRESHOLD = "package.save.threshold";
    @Property(
            name = DEFAULT_SAVE_THRESHOLD,
            label = "save threshold",
            intValue = 1024
    )
    protected int defaultSaveThreshold;

    @Override
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        Dictionary<String, Object> properties = context.getProperties();
        defaultSaveThreshold = PropertiesUtil.toInteger(properties.get(DEFAULT_SAVE_THRESHOLD), 1024);
    }

    @Override
    protected String getJobTopic() {
        return TOPIC;
    }

    @Override
    protected String getAutitBasePath() {
        return AUDIT_BASE_PATH;
    }

    @Override
    protected boolean jobExecutionEnabled(Job job) {
        return !Boolean.getBoolean("composum.never.start.pckgsvc");
    }

    @Override
    protected Callable<Object> createCallable(final Job job, final JobExecutionContext context,
                                              final ResourceResolver serviceResolver, final PrintWriter out)
            throws Exception {
        return new PackageManagerCallable(job, context, serviceResolver, out);
    }

    protected class PackageManagerCallable extends UserContextCallable {

        public PackageManagerCallable(final Job job, final JobExecutionContext context,
                                      final ResourceResolver serviceResolver, final PrintWriter out)
                throws RepositoryException, LoginException {
            super(job, context, serviceResolver, out);
        }

        @Override
        public Object call() throws Exception {
            JcrPackageManager manager = PackagingService.getPackageManager(session);
            JcrPackage jcrPckg = getJcrPackage(job, manager);
            String operation = (String) job.getProperty("operation");
            if (StringUtils.isNotBlank(operation)) {
                switch (operation.toLowerCase()) {
                    case "install":
                        installPackage(jcrPckg);
                        return null;
                    case "assemble":
                        return null;
                    case "rewrap":
                        return null;
                }
            } else {

            }
            return null;
        }

        protected void installPackage(JcrPackage jcrPckg)
                throws RepositoryException, IOException {

            ImportOptions options = createImportOptions();

            PackageProgressTracker tracker = createTracker();
            options.setListener(tracker);
            tracker.writePrologue();

            try {
                jcrPckg.install(options);
            } catch (PackageException pex) {
                LOG.error(pex.getMessage(), pex);
                throw new RepositoryException(pex);
            }
        }

        protected ImportOptions createImportOptions() {
            ImportOptions options = new ImportOptions();
            options.setDryRun(getProperty(job, JOB_PROPERTY_DRY_RUN, false));
            options.setAutoSaveThreshold(getProperty(job, JOB_PROPERTY_SAVE_THRESHOLD, defaultSaveThreshold));
            options.setImportMode(getProperty(job, JOB_PROPERTY_IMPORT_MODE, ImportMode.REPLACE));
            options.setHookClassLoader(dynamicClassLoaderManager.getDynamicClassLoader());
            return options;
        }

        protected JcrPackage getJcrPackage(Job job, JcrPackageManager manager)
                throws RepositoryException {
            JcrPackage jcrPackage = null;
            Node pckgRoot = manager.getPackageRoot();
            if (pckgRoot != null) {
                String reference = (String) job.getProperty(JOB_REFRENCE_PROPERTY);
                while (reference.startsWith("/")) {
                    reference = reference.substring(1);
                }
                Node pckgNode = pckgRoot.getNode(reference);
                if (pckgNode != null) {
                    jcrPackage = manager.open(pckgNode, true);
                }
            }
            return jcrPackage;
        }

        protected PackageProgressTracker createTracker()
                throws IOException {
            return new PackageProgressTracker.TextWriterTracking(out, IMPORT_DONE);
        }
    }
}
