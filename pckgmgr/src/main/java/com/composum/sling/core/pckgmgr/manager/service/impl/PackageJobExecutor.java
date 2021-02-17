package com.composum.sling.core.pckgmgr.manager.service.impl;

import com.composum.sling.core.concurrent.AbstractJobExecutor;
import com.composum.sling.core.concurrent.JobFailureException;
import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.pckgmgr.manager.util.PackageProgressTracker;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
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
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import static com.composum.sling.core.pckgmgr.manager.util.PackageUtil.IMPORT_DONE;

@Component(
        service = {JobExecutor.class, EventHandler.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Package Job Executor Service",
                JobExecutor.PROPERTY_TOPICS + "=" + PackageJobExecutor.TOPIC,
                EventConstants.EVENT_TOPIC + "=" + "org/apache/sling/event/notification/job/*"
        },
        immediate = true
)
@Designate(ocd = PackageJobExecutor.Configuration.class)
public class PackageJobExecutor extends AbstractJobExecutor<String> {

    private static final Logger LOG = LoggerFactory.getLogger(PackageJobExecutor.class);

    public static final String JOB_PROPERTY_DRY_RUN = "dryRun";
    public static final String JOB_PROPERTY_SAVE_THRESHOLD = "saveThreshold";
    public static final String JOB_PROPERTY_IMPORT_MODE = "importMode";

    public static final String TOPIC = "com/composum/sling/core/pckgmgr/PackageJobExecutor";

    public static final String AUDIT_BASE_PATH = AUDIT_ROOT_PATH + PackageJobExecutor.class.getName();

    protected final Lock lock = new ReentrantLock(true);

    @Reference
    private Packaging packaging;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SequencerService<SequencerService.Token> sequencer;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    private volatile Configuration config;

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
        this.config = configuration;
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

    @Nonnull
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
            options.setAutoSaveThreshold(getProperty(job, JOB_PROPERTY_SAVE_THRESHOLD, config.package_save_threshold()));
            options.setImportMode(getProperty(job, JOB_PROPERTY_IMPORT_MODE, ImportMode.REPLACE));
            options.setHookClassLoader(getDynamicClassLoaderManager().getDynamicClassLoader());
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
                return operationDone || ++waitLoopCount > config.package_progress_wait() * 2;
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

    @ObjectClassDefinition(name = "Composum Package Job Executor Service",
            description = "Provides the execution of package operations the repository context.")
    public @interface Configuration {
        @AttributeDefinition(name = "save threshold", description = "the auto-save threshold for the package import")
        int package_save_threshold() default 1024;

        @AttributeDefinition(name = "track idle time",
                description = "idle time in seconds for the progress tracker to check the operations end")
        int package_progress_wait() default 10;
    }

}
