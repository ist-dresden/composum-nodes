package com.composum.nodes.setup.impl;

import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.osgi.framework.Bundle.STARTING;
import static org.osgi.framework.Bundle.UNINSTALLED;

@Component(
        immediate = true,
        property = {
                JobConsumer.PROPERTY_TOPICS + "=" + NodesSetupService.JOB_TOPIC
        }
)
public class NodesSetupService implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NodesSetupService.class);

    public static final String JOB_TOPIC = "com/composum/nodes/setup";

    public static final String SETUP_EXT = ".setup";
    public static final String INSTALL_FOLDER = "install";
    public static final String UPLOAD_FOLDER = "upload";
    public static final String NODES_CONTENT_PATH = "/libs/composum/nodes";
    public static final String NODES_BUNDLES_PATH = NODES_CONTENT_PATH + "/" + INSTALL_FOLDER;
    public static final String SETUP_NODES_FOLDER = NODES_CONTENT_PATH + SETUP_EXT;
    public static final String SETUP_BUNDLES_PATH = SETUP_NODES_FOLDER + "/" + UPLOAD_FOLDER;
    public static final String NODES_PACKAGES_PATH = "com/composum/nodes/";
    public static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(.*/)?composum-nodes-package-setup-bundle-(?<version>(.+))\\.jar$");

    public static final Pattern[] INSTALL_ARTIFACTS = new Pattern[]{
            Pattern.compile("^composum-nodes-package-setup-bundle-(.+)\\.jar$"),
            Pattern.compile("^.*\\.LoginAdminWhitelist\\.fragment-composum_nodes_setup\\.config$")
    };

    public static final Pattern[] BUNDLES_TO_UNINSTALL = new Pattern[]{
            Pattern.compile("^(.*/)?composum-nodes-jslibs-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-core-jslibs-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-usermgr-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-user-management-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-pckgmgr-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-package-manager-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-console-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-core-console-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-config-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-core-config-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-commons-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-core-commons-.*\\.jar$")
    };

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    protected JobManager jobManager;

    @Reference
    private Packaging packaging;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        LOG.info("Composum Nodes setup.activate...");
        Job job = jobManager.createJob(JOB_TOPIC).add();
        LOG.info("Composum Nodes setup: job created: [ {} ]", job.getId());
    }

    @Deactivate
    protected void deactivate() {
        LOG.info("Composum Nodes setup.deactivate...");
        bundleContext = null;
    }

    @Override
    public JobResult process(Job job) {
        wait(1);
        LOG.info("\n\nComposum Nodes setup: processing...\n");
        try (ResourceResolver resolver = resolverFactory.getAdministrativeResourceResolver(null)) {
            Session session = resolver.adaptTo(Session.class);
            if (session != null) {
                try {
                    // check for the setup folder and perform installation if found
                    Node nodesSetupFolder = session.getNode(SETUP_NODES_FOLDER);
                    BundleContext bundleContext = this.bundleContext;
                    if (removeNodesBundles(session)) {
                        wait(3); // wait if bundles has been removed; maybe this bundle is also restarted...
                    }
                    // check for new activation of the setup bundle during bundle removal
                    if (bundleContext.equals(this.bundleContext)) {
                        setupNodesBundlesAndContent(session);
                        LOG.info("\n\nComposum Nodes setup: process succeeded.\n");
                        try {
                            // final removal of the Nodes setup artifacts (setup-bundle and the configuration)
                            removeInstallArtifacts(session);
                            session.save();
                        } catch (RepositoryException exx) {
                            LOG.error("Composum Nodes setup: " + exx.toString());
                        }
                    } else {
                        LOG.info("\n\nComposum Nodes setup: process stopped - waiting for a following job.\n");
                    }
                } catch (PathNotFoundException ignore) {
                    LOG.info("\n\nComposum Nodes setup: no Nodes install folder found [ {} ]\n", SETUP_NODES_FOLDER);
                } catch (RepositoryException | PackageException | IOException | RuntimeException ex) {
                    LOG.error("Composum Nodes setup: " + ex.getMessage(), ex);
                    LOG.error("\n\nComposum Nodes setup: process failed!\n");
                }
            } else {
                LOG.error("Composum Nodes setup: can't adapt to Session, failed!");
            }
        } catch (LoginException lex) {
            LOG.error("Composum Nodes setup: " + lex.getMessage());
            LOG.error("\n\nComposum Nodes setup: process failed!\n");
        }
        return JobResult.OK; // always OK, no retry!
    }

    protected boolean removeNodesBundles(@NotNull final Session session)
            throws RepositoryException {
        boolean bundlesRemoved = false;
        try {
            // check for the setup folder and perform installation if found
            Node nodesSetupFolder = session.getNode(SETUP_NODES_FOLDER);
            // remove existing bundles and install new bundles
            bundlesRemoved = removeUploadedBundles(session);
            if (bundlesRemoved) {
                wait(3);
            }
            bundlesRemoved = uninstallBundles() || bundlesRemoved;
        } catch (PathNotFoundException ignore) {
            LOG.info("\n\nComposum Nodes setup: no Nodes install folder found [ {} ]\n", SETUP_NODES_FOLDER);
        }
        return bundlesRemoved;
    }

    /**
     * setup new Nodes bundles and content installed in the setup folder during package install
     *
     * @param session the JCR session of the executed setup job
     * @throws RepositoryException ...
     */
    protected void setupNodesBundlesAndContent(@NotNull final Session session)
            throws RepositoryException, PackageException, IOException {
        try {
            boolean bundlesRemoved = false;
            // check for the setup folder and perform installation if found
            Node nodesSetupFolder = session.getNode(SETUP_NODES_FOLDER);
            try {
                Node nodesSetupBundles = session.getNode(SETUP_BUNDLES_PATH);
                // setup of the new nodes bundles (move from 'upload' to the 'install' folder)
                NodeIterator iterator = nodesSetupBundles.getNodes();
                while (iterator.hasNext()) {
                    Node node = iterator.nextNode();
                    String targetPath = NODES_BUNDLES_PATH + "/" + node.getName();
                    LOG.info("Composum Nodes setup: installing node [ {} ]", targetPath);
                    session.move(node.getPath(), targetPath);
                }
                session.save();
                // remove the empty 'upload' folder
                nodesSetupBundles.remove();
                session.save();
            } catch (PathNotFoundException ignore) {
            }
            // remove the content resources
            removeNodesContent(session);
            // setup of the new Nodes content resources (move from 'nodes.setup' to 'nodes')
            NodeIterator iterator = nodesSetupFolder.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                String targetPath = NODES_CONTENT_PATH + "/" + node.getName();
                LOG.info("Composum Nodes setup: installing node [ {} ]", targetPath);
                session.move(node.getPath(), targetPath);
            }
            session.save();
            // remove the empty 'nodes.setup' folder
            nodesSetupFolder.remove();
            session.save();
            // it's possible that 'inital content' has been deleted triggered by a bundle removal
            // reinstall the libraries content subpackage if a bundle has been removed...
            Matcher matcher = VERSION_PATTERN.matcher(bundleContext.getBundle().getLocation());
            if (matcher.matches()) {
                wait(3);
                String version = matcher.group("version");
                installPackage(session, NODES_PACKAGES_PATH + "composum-nodes-jslibs-package-" + version + ".zip");
            }
        } catch (PathNotFoundException ignore) {
            LOG.info("\n\nComposum Nodes setup: no Nodes install folder found [ {} ]\n", SETUP_NODES_FOLDER);
        }
    }

    /**
     * (re-)install a package or subpackage if subpackages are specified
     *
     * @param session         the curren session
     * @param packagePath     the relative path of the package to install
     * @param subpckgPatterns a set of subpackage name patterns, eachb matchin subpackage will be installed
     */
    protected void installPackage(@NotNull final Session session, @NotNull final String packagePath,
                                  @NotNull final Pattern... subpckgPatterns)
            throws RepositoryException, PackageException, IOException {
        JcrPackageManager manager = packaging.getPackageManager(session);
        Node pckgRoot = manager.getPackageRoot();
        Node pckgNode = pckgRoot.getNode(packagePath);
        if (pckgNode != null) {
            JcrPackage jcrPackage = manager.open(pckgNode, true);
            if (jcrPackage != null) {
                ImportOptions importOptions = createPackageImportOptions();
                if (subpckgPatterns == null || subpckgPatterns.length < 1) {
                    LOG.info("Composum Nodes setup: package install [ {} ]", packagePath);
                    jcrPackage.install(importOptions);
                } else {
                    PackageId[] subpackages = jcrPackage.extractSubpackages(importOptions);
                    for (PackageId pckgId : subpackages) {
                        JcrPackage subpackage = manager.open(pckgId);
                        if (subpackage != null) {
                            for (Pattern pattern : subpckgPatterns) {
                                Node subpckgNode = subpackage.getNode();
                                if (subpckgNode != null) {
                                    String subpckgName = subpckgNode.getName();
                                    if (pattern.matcher(subpckgName).matches()) {
                                        LOG.info("Composum Nodes setup: subpackage install [ {} ]", subpckgName);
                                        subpackage.install(importOptions);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected ImportOptions createPackageImportOptions() {
        ImportOptions options = new ImportOptions();
        options.setDryRun(false);
        options.setAutoSaveThreshold(1024);
        options.setImportMode(ImportMode.REPLACE);
        options.setHookClassLoader(dynamicClassLoaderManager.getDynamicClassLoader());
        return options;
    }

    /**
     * remove the Nodes modules content resources to prepare a clean install
     */
    protected void removeNodesContent(@NotNull final Session session)
            throws RepositoryException {
        try {
            Node nodesContent = session.getNode(NODES_CONTENT_PATH);
            // remove existing content
            NodeIterator iterator = nodesContent.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (!INSTALL_FOLDER.equals(node.getName())) {
                    LOG.info("Composum Nodes setup: removing node [ {} ]", node.getPath());
                    node.remove();
                }
            }
            session.save();
        } catch (PathNotFoundException ignore) {
        }
    }

    /**
     * removes all matching artifacts in the bundle install repository folder (removes bundle files uploaded before)
     *
     * @return true if at least one artifact has been removed
     */
    protected boolean removeUploadedBundles(@NotNull final Session session) {
        boolean result = false;
        try {
            Node bundlesFolder = session.getNode(NODES_BUNDLES_PATH);
            NodeIterator iterator = bundlesFolder.getNodes();
            while (iterator.hasNext()) {
                try {
                    Node bundleNode = iterator.nextNode();
                    if (!isInstallArtifact(bundleNode.getName())) {
                        LOG.info("Composum Nodes setup: removing node [ {} ]", bundleNode.getPath());
                        bundleNode.remove();
                        result = true;
                    }
                } catch (RepositoryException ex) {
                    LOG.error(ex.toString());
                }
            }
            session.save();
        } catch (PathNotFoundException ignore) {
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * uninstalls all bundles matching to the unistall pattern list (removes bundles installed before)
     *
     * @return true if at least one bundle has been uninstalled
     */
    protected boolean uninstallBundles() {
        boolean result = false;
        for (Pattern pattern : BUNDLES_TO_UNINSTALL) {
            /* if setup bundle is deactivated while job processing the bundle content is 'null'! */
            if (bundleContext != null) {
                for (Bundle bundle : bundleContext.getBundles()) {
                    Matcher matcher = pattern.matcher(bundle.getLocation());
                    if (matcher.matches()) {
                        try {
                            int state = bundle.getState();
                            if (state != UNINSTALLED) {
                                result = true;
                                LOG.info("Composum Nodes setup: uninstalling bundle [ {} ]", bundle.getLocation());
                                if (state == Bundle.ACTIVE || state == STARTING) {
                                    bundle.stop();
                                }
                                bundle.uninstall();
                            }
                        } catch (BundleException | IllegalStateException ex) {
                            LOG.error(ex.toString());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return 'true' if the name references an artifact od the setup itself
     */
    protected boolean isInstallArtifact(@NotNull final String name) {
        for (Pattern pattern : INSTALL_ARTIFACTS) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * the final removal of the install artifacts (setup-bundle and the related configuration)
     */
    protected void removeInstallArtifacts(@NotNull final Session session)
            throws RepositoryException {
        try {
            Node bundlesFolder = session.getNode(NODES_BUNDLES_PATH);
            NodeIterator iterator = bundlesFolder.getNodes();
            while (iterator.hasNext()) {
                try {
                    Node bundleNode = iterator.nextNode();
                    if (isInstallArtifact(bundleNode.getName())) {
                        LOG.info("Composum Nodes setup: removing install node [ {} ]", bundleNode.getPath());
                        bundleNode.remove();
                    }
                } catch (RepositoryException ex) {
                    LOG.error(ex.toString());
                }
            }
        } catch (PathNotFoundException ignore) {
        }
    }

    protected void wait(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ex) {
            LOG.warn(ex.toString());
        }
    }
}
