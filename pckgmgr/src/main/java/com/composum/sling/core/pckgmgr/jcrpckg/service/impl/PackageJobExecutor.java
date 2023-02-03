package com.composum.sling.core.pckgmgr.jcrpckg.service.impl;

import com.composum.sling.core.concurrent.AbstractJobExecutor;
import com.composum.sling.core.concurrent.JobFailureException;
import com.composum.sling.core.concurrent.SequencerService;
import com.composum.sling.core.pckgmgr.jcrpckg.util.PackageProgressTracker;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.util.RegistryUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.DependencyHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.registry.ExecutionPlan;
import org.apache.jackrabbit.vault.packaging.registry.ExecutionPlanBuilder;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;
import org.apache.jackrabbit.vault.packaging.registry.PackageTask;
import org.apache.jackrabbit.vault.packaging.registry.RegisteredPackage;
import org.apache.jackrabbit.vault.packaging.registry.impl.FSPackageRegistry;
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

import static com.composum.sling.core.pckgmgr.jcrpckg.util.PackageUtil.IMPORT_DONE;
import static java.util.Objects.requireNonNull;

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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private PackageRegistries packageRegistries;

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
                String reference = (String) job.getProperty(JOB_REFRENCE_PROPERTY);
                String operation = requireNonNull((String) job.getProperty("operation"), "No operation requested!");
                if (RegistryUtil.isRegistryBasedPath(reference)) {
                    return new RegistryOperation(reference, operation).call();
                } else {
                    JcrPackageManager manager = packaging.getPackageManager(session);
                    JcrPackage jcrPckg = getJcrPackage(manager, reference);
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
                }
            } finally {
                lock.unlock();
                close();
            }
        }

        protected class InstallOperation extends JcrPackageOperation {

            public InstallOperation(JcrPackageManager manager, JcrPackage jcrPckg)
                    throws IOException {
                super(manager, jcrPckg, true);
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

        protected class AssembleOperation extends JcrPackageOperation {

            public AssembleOperation(JcrPackageManager manager, JcrPackage jcrPckg)
                    throws IOException {
                super(manager, jcrPckg, false);
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

        protected class UninstallOperation extends JcrPackageOperation {

            public UninstallOperation(JcrPackageManager manager, JcrPackage jcrPckg)
                    throws IOException {
                super(manager, jcrPckg, true);
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
            options.setDependencyHandling(DependencyHandling.BEST_EFFORT);
            return options;
        }

        protected class RegistryOperation extends AbstractPackageOperation {

            protected final String reference;
            protected final PackageTask.Type type;
            protected ExecutionPlan plan;

            public RegistryOperation(String reference, String operation) throws IllegalArgumentException {
                super(false);
                this.reference = reference;
                this.type = convertOperation(operation);
            }

            protected PackageTask.Type convertOperation(String operation) {
                switch (operation.toLowerCase()) {
                    case "install":
                        return PackageTask.Type.INSTALL;
                    case "uninstall":
                        return PackageTask.Type.UNINSTALL;
                    case "extract":
                        return PackageTask.Type.EXTRACT;
                    case "remove":
                        return PackageTask.Type.REMOVE;
                    default:
                        throw new IllegalArgumentException("Unsupported operation: " + operation);
                }
            }

            @Override
            protected void doIt() throws PackageException, IOException, RepositoryException {
                PackageRegistries.Registries registries = packageRegistries.getRegistries(serviceResolver);
                Pair<String, PackageId> pckgid = requireNonNull(registries.resolve(reference), "Can't find package " + reference);
                PackageRegistry registry = registries.getRegistry(pckgid.getLeft());
                PackageTask.Type taskType = fixTaskType(pckgid, registry, type);
                taskType = fixTaskType(pckgid, registry, taskType);
                ExecutionPlanBuilder builder = registry.createExecutionPlan()
                        .with(session)
                        .with(this.tracker)
                        .addTask().with(pckgid.getRight()).with(taskType);
                builder.validate();
                // TODO(hps,10.05.22) possibly handle DependencyException thrown in validate more sensibly. Add them automatically?
                // registry.analyzeDependencies(pckgid.getRight(), false)? How to handle transitive?
                plan = builder.execute();
                evaluatePlan(plan);
            }

            /** On a FSPackageRegistry "INSTALL" isn't available. But it seems really hard to recognize it. */
            private PackageTask.Type fixTaskType(Pair<String, PackageId> pckgid, PackageRegistry registry, PackageTask.Type taskType) throws IOException {
                if (this.type == PackageTask.Type.INSTALL) {
                    RegisteredPackage regpckg = registry.open(pckgid.getRight());
                    if (regpckg != null && regpckg.getPackage() != null && regpckg.getPackage().getFile() != null) {
                        LOG.info("We are assuming this is a FSPackageRegistry and that supports EXTRACT but not INSTALL for {}", pckgid);
                        taskType = PackageTask.Type.EXTRACT;
                    }
                }
                return taskType;
            }

            protected void evaluatePlan(ExecutionPlan plan) throws PackageException, IOException, RepositoryException {
                if (plan.isExecuted()) {
                    String msg = plan.hasErrors() ? "with errors" : "without errors";
                    tracker.onMessage(ProgressTrackerListener.Mode.TEXT, plan.getId(), "Plan was executed " + msg);
                }

                Throwable throwable = null;
                if (plan.hasErrors()) {
                    for (PackageTask task : plan.getTasks()) {
                        if (task.getError() != null) {
                            tracker.onError(ProgressTrackerListener.Mode.TEXT, task.getError().getMessage(), (Exception) task.getError());
                            if (throwable == null) {
                                throwable = task.getError();
                            }
                        }
                    }
                }

                if (throwable instanceof PackageException) {
                    throw (PackageException) throwable;
                } else if (throwable instanceof IOException) {
                    throw (IOException) throwable;
                } else if (throwable instanceof RepositoryException) {
                    throw (RepositoryException) throwable;
                } else if (throwable != null) {
                    throw new PackageException(throwable);
                }
            }

            @Override
            protected boolean isDone() {
                return (plan != null && plan.isExecuted() && !plan.hasErrors()) || super.isDone();
            }
        }

        protected JcrPackage getJcrPackage(JcrPackageManager manager, String reference)
                throws RepositoryException {
            JcrPackage jcrPackage = null;
            Node pckgRoot = manager.getPackageRoot();
            if (pckgRoot != null) {
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

        protected abstract class AbstractPackageOperation implements Callable<String> {

            protected final OperationDoneTracker tracker;

            /** Should be true when the operation is sensibly tracked so that we can determine when it's finished. */
            protected final boolean hasFinishLogMessage;

            protected AbstractPackageOperation(boolean hasFinishLogMessage) {
                tracker = new OperationDoneTracker(out, IMPORT_DONE);
                this.hasFinishLogMessage = hasFinishLogMessage;
            }

            protected abstract void doIt() throws PackageException, IOException, RepositoryException;

            protected void track() {
                while (!isDone()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException iex) {
                        LOG.info("Operation track interrupted: {}", iex.getMessage());
                    }
                }
            }

            protected boolean isDone() {
                return !hasFinishLogMessage || tracker.isOperationDone();
            }

            protected String done() throws IOException {
                return "done.";
            }

            @Override
            public String call() throws IOException, RepositoryException, PackageException {
                tracker.writePrologue();
                doIt();
                track();
                return done();
            }

        }

        protected abstract class JcrPackageOperation extends AbstractPackageOperation {

            public final JcrPackageManager manager;
            public final JcrPackage jcrPckg;
            public final ImportOptions options;

            protected JcrPackageOperation(JcrPackageManager manager, JcrPackage jcrPckg, boolean isTracked) {
                super(isTracked);
                this.manager = manager;
                this.jcrPckg = jcrPckg;
                options = createImportOptions();
                options.setListener(tracker);
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
