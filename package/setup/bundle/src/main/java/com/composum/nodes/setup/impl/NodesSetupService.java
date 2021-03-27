package com.composum.nodes.setup.impl;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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

    public static final Pattern[] INSTALL_ARTIFACTS = new Pattern[]{
            Pattern.compile("^composum-nodes-package-setup-bundle-(.+)\\.jar$"),
            Pattern.compile("^.*\\.LoginAdminWhitelist\\.fragment-composum_nodes_setup\\.config$")
    };

    public static final Pattern[] BUNDLES_TO_UNINSTALL = new Pattern[]{
            Pattern.compile("^(.*/)?composum-nodes-usermgr-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-pckgmgr-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-console-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-commons-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-nodes-config-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-user-management-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-package-manager-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-core-console-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-core-commons-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-core-jslibs-.*\\.jar$"),
            Pattern.compile("^(.*/)?composum-sling-core-config-.*\\.jar$")
    };

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    protected JobManager jobManager;

    private BundleContext bundleContext;

    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        Job job = jobManager.createJob(JOB_TOPIC).add();
        LOG.info("Nodes setup: job created: [ {} ]", job);
    }

    @Override
    public JobResult process(Job job) {
        LOG.info("Nodes setup: process...");
        try (ResourceResolver resolver = resolverFactory.getAdministrativeResourceResolver(null)) {
            Session session = resolver.adaptTo(Session.class);
            if (session != null) {
                setupNodesBundlesAndContent(session);
                LOG.info("Nodes setup: process succeeded.");
                return JobResult.OK;
            } else {
                LOG.error("Nodes setup: can't adapt to Session");
            }
        } catch (RepositoryException | RuntimeException ex) {
            LOG.error(ex.getMessage(), ex);
        } catch (LoginException lex) {
            LOG.error(lex.getMessage());
        }
        LOG.info("Nodes setup: process failed!");
        return JobResult.OK; // always OK, no retry!
    }

    /**
     * setup new Nodes bundles and content installed in the setup folder during package install
     *
     * @param session the JCR session of the executed setup job
     * @throws RepositoryException ...
     */
    protected void setupNodesBundlesAndContent(@NotNull final Session session)
            throws RepositoryException {
        try {
            boolean bundlesRemoved = false;
            // check for the setup folder and perform installation if found
            Node nodesSetupFolder = session.getNode(SETUP_NODES_FOLDER);
            try {
                Node nodesSetupBundles = session.getNode(SETUP_BUNDLES_PATH);
                // remove existing bundles and install new bundles
                bundlesRemoved = removeNodesBundles(session);
                NodeIterator iterator = nodesSetupBundles.getNodes();
                while (iterator.hasNext()) {
                    Node node = iterator.nextNode();
                    String targetPath = NODES_BUNDLES_PATH + "/" + node.getName();
                    LOG.info("Nodes setup: installing node [ {} ]", targetPath);
                    session.move(node.getPath(), targetPath);
                }
                nodesSetupBundles.remove();
                session.save();
            } catch (PathNotFoundException ignore) {
            }
            removeNodesContent(session);
            NodeIterator iterator = nodesSetupFolder.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                String targetPath = NODES_CONTENT_PATH + "/" + node.getName();
                LOG.info("Nodes setup: installing node [ {} ]", targetPath);
                session.move(node.getPath(), targetPath);
            }
            session.save();
            if (bundlesRemoved) {
                // it's possible that content has been deleted triggered by a bundle removal
                // reinstall the libraries content of the bundled initial content...
                // FIXME...
            }
            nodesSetupFolder.remove();
            removeInstallArtifacts(session);
            session.save();
        } catch (PathNotFoundException ignore) {
            LOG.info("Nodes setup: no Nodes install folder found [ {} ]", SETUP_NODES_FOLDER);
        }
    }

    protected void removeNodesContent(@NotNull final Session session)
            throws RepositoryException {
        try {
            Node nodesContent = session.getNode(NODES_CONTENT_PATH);
            // remove existing content
            NodeIterator iterator = nodesContent.getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                if (!INSTALL_FOLDER.equals(node.getName())) {
                    LOG.info("Nodes setup: removing node [ {} ]", node.getPath());
                    node.remove();
                }
            }
            session.save();
        } catch (PathNotFoundException ignore) {
        }
    }

    /**
     * removes each installed bundle or install resource of the Nodes modules
     *
     * @return true if at least one resource or bundle has been removed
     */
    protected boolean removeNodesBundles(@NotNull final Session session) {
        boolean result = removeUploadedBundles(session);
        if (result) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ex) {
                LOG.warn(ex.toString());
            }
        }
        return uninstallBundles() || result;
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
                        LOG.info("Nodes setup: removing node [ {} ]", bundleNode.getPath());
                        bundleNode.remove();
                        result = true;
                    }
                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
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
        for (Bundle bundle : bundleContext.getBundles()) {
            if (isUninstallBundle(bundle.getLocation())) {
                try {
                    int state = bundle.getState();
                    if (state != UNINSTALLED) {
                        LOG.info("Nodes setup: uninstalling bundle [ {} ]", bundle.getLocation());
                        if (state == Bundle.ACTIVE || state == STARTING) {
                            bundle.stop();
                        }
                        bundle.uninstall();
                    }
                    result = true;
                } catch (BundleException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
        return result;
    }

    protected boolean isUninstallBundle(@NotNull final String location) {
        for (Pattern pattern : BUNDLES_TO_UNINSTALL) {
            Matcher matcher = pattern.matcher(location);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInstallArtifact(@NotNull final String name) {
        for (Pattern pattern : INSTALL_ARTIFACTS) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    protected void removeInstallArtifacts(@NotNull final Session session)
            throws RepositoryException {
        try {
            Node bundlesFolder = session.getNode(NODES_BUNDLES_PATH);
            NodeIterator iterator = bundlesFolder.getNodes();
            while (iterator.hasNext()) {
                try {
                    Node bundleNode = iterator.nextNode();
                    if (isInstallArtifact(bundleNode.getName())) {
                        LOG.info("Nodes setup: removing install node [ {} ]", bundleNode.getPath());
                        bundleNode.remove();
                    }
                } catch (RepositoryException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        } catch (PathNotFoundException ignore) {
        }
    }
}
