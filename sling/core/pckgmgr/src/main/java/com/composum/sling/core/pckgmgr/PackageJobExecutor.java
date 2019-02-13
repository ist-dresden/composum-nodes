package com.composum.sling.core.pckgmgr;

import com.composum.sling.core.concurrent.AbstractJobExecutor;
import com.composum.sling.core.concurrent.JobFailureException;
import com.composum.sling.core.pckgmgr.util.PackageProgressTracker;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

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
public class PackageJobExecutor extends AbstractJobExecutor<String> {

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

    public static final String PROGRESS_TRACK_IDLE_TIME = "package.progress.wait";
    @Property(
            name = PROGRESS_TRACK_IDLE_TIME,
            label = "track idle time",
            description = "idle time in seconds for the progress tracker to check the operations end",
            intValue = 10
    )
    protected int progressTrackIdleTime;

    protected final Lock lock = new ReentrantLock(true);

    @Reference
    private Packaging packaging;

    @Override
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        Dictionary<String, Object> properties = context.getProperties();
        defaultSaveThreshold = PropertiesUtil.toInteger(properties.get(DEFAULT_SAVE_THRESHOLD), 1024);
        progressTrackIdleTime = PropertiesUtil.toInteger(properties.get(PROGRESS_TRACK_IDLE_TIME), 10);
    }

    @Override
    protected String getJobTopic() {
        return TOPIC;
    }

    @Override
    protected String getAuditBasePath() {
        return AUDIT_BASE_PATH;
    }

    @Override
    protected boolean jobExecutionEnabled(Job job) {
        return !Boolean.getBoolean("composum.never.start.pckgsvc");
    }

    @Override
    protected Callable<String> createCallable(final Job job, final JobExecutionContext context,
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
        public String call() throws Exception {
            lock.lock();
            try {
                JcrPackageManager manager = packaging.getPackageManager(session);
                JcrPackage jcrPckg = getJcrPackage(job, manager);
                String operation = (String) job.getProperty("operation");
                if (StringUtils.isNotBlank(operation)) {
                    switch (operation.toLowerCase()) {
                        case "install":
                            final String name = jcrPckg.getPackage().getId().getName();
                            LOG.info("start of installation of package '{}'", name);
                            final String call = new InstallOperation(manager, jcrPckg).call();
                            LOG.info("installation of package '{}' done", name);
                            return call;
                        case "assemble":
                            return new AssembleOperation(manager, jcrPckg).call();
                        case "uninstall":
                            return new UninstallOperation(manager, jcrPckg).call();
                        default:
                            throw new Exception("Unsupported operation: " + operation);
                    }
                } else {
                    throw new Exception("No operation requested!");
                }
            } finally {
                lock.unlock();
                close();
            }
        }

        protected class InstallOperation extends TrackedOperation {

            public InstallOperation(JcrPackageManager manager, JcrPackage jcrPckg)
                    throws IOException {
                super(manager, jcrPckg);
            }

            @Override
            protected void doIt() throws PackageException, IOException, RepositoryException {
                jcrPckg.install(options);
            }

            @Override
            protected String done() throws IOException {
                if (tracker.getErrorDetected()) {
                    throw new JobFailureException("install done with errors");
                }
                return super.done();
            }
        }

        protected class AssembleOperation extends Operation {

            public AssembleOperation(JcrPackageManager manager, JcrPackage jcrPckg)
                    throws IOException {
                super(manager, jcrPckg);
            }

            @Override
            protected void doIt() throws PackageException, IOException, RepositoryException {
                manager.assemble(jcrPckg, tracker);
            }

            @Override
            protected String done() throws IOException {
                return "Package assembled.";
            }
        }

        protected class UninstallOperation extends TrackedOperation {

            public UninstallOperation(JcrPackageManager manager, JcrPackage jcrPckg)
                    throws IOException {
                super(manager, jcrPckg);
            }

            @Override
            protected void doIt() throws PackageException, IOException, RepositoryException {
                jcrPckg.uninstall(options);
            }

            @Override
            protected String done() throws IOException {
                return "Package uninstall done.";
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

        protected abstract class Operation implements Callable<String> {

            public final JcrPackageManager manager;
            public final JcrPackage jcrPckg;
            public final ImportOptions options;
            public final OperationDoneTracker tracker;

            public Operation(JcrPackageManager manager, JcrPackage jcrPckg)
                    throws IOException {
                this.manager = manager;
                this.jcrPckg = jcrPckg;
                tracker = new OperationDoneTracker(out, IMPORT_DONE);
                options = createImportOptions();
                options.setListener(tracker);
            }

            protected abstract void doIt() throws PackageException, IOException, RepositoryException;

            protected void track() {
            }

            protected String done() throws IOException {
                return "done.";
            }

            @Override
            public String call() throws IOException, RepositoryException {
                tracker.writePrologue();
                try {
                    doIt();
                    track();
                    return done();
                } catch (PackageException pex) {
                    LOG.error(pex.getMessage(), pex);
                    throw new RepositoryException(pex);
                }
            }
        }

        protected abstract class TrackedOperation extends Operation {

            public TrackedOperation(JcrPackageManager manager, JcrPackage jcrPckg)
                    throws IOException {
                super(manager, jcrPckg);
            }

            @Override
            protected void track() {
                while (!tracker.isOperationDone()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException iex) {
                        LOG.info("Operation track interrupted: " + iex.getMessage());
                    }
                }
            }
        }

        protected class OperationDoneTracker extends PackageProgressTracker.TextWriterTracking {

            protected boolean operationDone;
            protected int waitLoopCount;

            public OperationDoneTracker(PrintWriter writer, Pattern finalizedIndicator) {
                super(writer, finalizedIndicator);
            }

            public boolean isOperationDone() {
                return operationDone || ++waitLoopCount > progressTrackIdleTime * 2;
            }

            @Override
            public void writeEpilogue() throws IOException {
                super.writeEpilogue();
                operationDone = true;
            }

            @Override
            protected void writeItem(Item item) throws IOException {
                super.writeItem(item);
                waitLoopCount = 0;
            }
        }
    }
}
