package com.composum.sling.core.pckginstall;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PackageTransformer implements ResourceTransformer, InstallTaskFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private BundleContext bundleContext;

    @Reference
    private Packaging packaging;

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public TransformationResult[] transform(RegisteredResource resource) {
        final String type = resource.getType();
        if (type != null && type.equals("file") && resource.getURL().endsWith(".zip")) {
            try {
                final Manifest m = getManifest(resource);
                if (m != null) {
                    final String version = m.getMainAttributes().getValue("Implementation-Version");
                    if (version != null) {
                        final String title = m.getMainAttributes().getValue("Implementation-Title");
                        logger.info("transforming of package '{}' with installation.hint '{}'", title, resource.getDictionary().get(InstallableResource.INSTALLATION_HINT));
                        TransformationResult tr = new TransformationResult();
                        tr.setResourceType("package");
                        final Map<String, Object> attr = new HashMap<>();
                        tr.setAttributes(attr);
                        tr.setId(title);
                        Version v = new Version(cleanupVersion(version));
                        tr.setVersion(v);
                        return new TransformationResult[]{tr};
                    }
                }
            } catch (Exception e) {
                logger.warn("Exception transforming RegisteredResource: " + e, e);
            }
        }
        return new TransformationResult[0];
    }

    private final static Pattern VERSION_PATTERN = Pattern.compile("(\\d+)?(\\.\\d+)?(\\.\\d+(?=-|\\.|$))?[-.]?(.*)");

    // Transform into valid OSGI version.
    protected static String cleanupVersion(String version) {
        Matcher m = VERSION_PATTERN.matcher(version);
        if (m.matches()) {
            StringBuilder buf = new StringBuilder();
            String grp = m.group(1);
            if (grp != null && !grp.isEmpty()) {
                buf.append(grp);
            } else {
                buf.append("0");
            }
            grp = m.group(2);
            if (grp != null && !grp.isEmpty()) {
                buf.append(grp);
            } else {
                buf.append(".0");
            }
            grp = m.group(3);
            if (grp != null && !grp.isEmpty()) {
                buf.append(grp);
            } else {
                buf.append(".0");
            }
            grp = m.group(4);
            if (grp != null && !grp.isEmpty()) {
                buf.append(".").append(m.group(4).replaceAll("[^a-zA-Z0-9-]", "_"));
            }
            return buf.toString();
        } else { // bug - should be impossible.
            throw new IllegalArgumentException("Unparseable version number " + version);
        }
    }

    @Override
    public InstallTask createTask(TaskResourceGroup toActivate) {
        if (!toActivate.getActiveResource().getType().equals("package")) {
            return null;
        }
        logger.info("create PackageInstallTask for task resource with entityId '{}'", toActivate.getActiveResource().getEntityId());
        return new PackageInstallTask(toActivate);
    }

    private class PackageInstallTask extends InstallTask {

        PackageInstallTask(TaskResourceGroup r) {
            super(r);
        }

        private <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
            ServiceReference<S> reference = null;
            int waits = 0;
            while (reference == null) {
                reference = bundleContext.getServiceReference(clazz);
                if (reference == null && waits < 60) {
                    logger.warn("unable to get ServiceReference of {} - will retry in 5 sec.", clazz.getName());
                    waits++;
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                } else if (waits >= 60) {
                    logger.error("unable to get ServiceReference of {} - giving up", clazz.getName());
                    logger.error("installation will not be valid");
                }
            }
            return reference;
        }

        @Override
        public void execute(InstallationContext ctx) {
            ServiceReference<Repository> repositoryReference = null;
            ServiceReference<JobManager> jmRef = null;
            Session session = null;
            try {
                repositoryReference = getServiceReference(Repository.class);
                final Repository repository = bundleContext.getService(repositoryReference);
                final Method loginAdministrative = repository.getClass().getMethod("loginAdministrative", String.class);
                final Object invoke = loginAdministrative.invoke(repository, (Object) null);
                session = (Session) invoke;
                final JcrPackageManager packageManager = packaging.getPackageManager(session);
                final TaskResource resource = getResource();
                final InputStream inputStream = resource.getInputStream();
                logger.info("package upload - {}", resource.getEntityId());
                final JcrPackage jcrPackage = packageManager.upload(inputStream, true, true);

                jmRef = getServiceReference(JobManager.class);
                final JobManager jm = bundleContext.getService(jmRef);
                String path = jcrPackage.getNode().getPath();
                final String root = packageManager.getPackageRoot().getPath();
                if (path.startsWith(root)) {
                    path = path.substring(root.length());
                }

                final Map<String, Object> jobProperties = new HashMap<>();
                jobProperties.put("reference", path);
                jobProperties.put("operation", "install");
                jobProperties.put("userid", session.getUserID());
                buildOutfileName(jobProperties);
                logger.info("add package install job with path '{}' and sort key ‘{}‘", path, getSortKey());
                final Job job = jm.addJob("com/composum/sling/core/pckgmgr/PackageJobExecutor", jobProperties);
                session.save();
                this.setFinishedState(ResourceState.INSTALLED);
            } catch (Exception e) {
                logger.warn("Exception executing PackageInstallTask: " + e, e);
            } finally {
                if (jmRef != null) {
                    bundleContext.ungetService(jmRef);
                }
                if (repositoryReference != null) {
                    bundleContext.ungetService(repositoryReference);
                }
                if (session != null) {
                    session.logout();
                }
            }
        }

        @Override
        public String getSortKey() {
            return "60-" + getSortableStartLevel() + "-" + getResource().getEntityId();
        }

        /**
         * Get sortable start level - low levels before high levels
         */
        private String getSortableStartLevel() {
            final Object hint = getResource().getDictionary().get(InstallableResource.INSTALLATION_HINT);
            int startLevel;
            if (hint == null) {
                startLevel = 0;
            } else {
                try {
                    startLevel = Integer.parseInt(String.valueOf(hint));
                } catch (NumberFormatException e) {
                    startLevel = 0;
                }
            }

            if (startLevel == 0) {
                return "999";
            } else if (startLevel < 10) {
                return "00" + String.valueOf(startLevel);
            } else if (startLevel < 100) {
                return "0" + String.valueOf(startLevel);
            }
            return String.valueOf(startLevel);
        }

    }

    /**
     * Read the manifest from supplied input stream, which is closed before return.
     */
    private static Manifest getManifest(final RegisteredResource rsrc) throws IOException {
        try (final InputStream ins = rsrc.getInputStream()) {
            if (ins != null) {
                try (JarInputStream jis = new JarInputStream(ins)) {
                    return jis.getManifest();
                }
            } else {
                return null;
            }
        }
    }

    private static String buildOutfileName(Map<String, Object> properties) {
        final String tmpdir = System.getProperty("java.io.tmpdir");
        final boolean endsWithSeparator = (tmpdir.charAt(tmpdir.length() - 1) == File.separatorChar);
        String outfile;
        outfile = tmpdir + (endsWithSeparator ? "" : File.separator) + "slingjob_" + System.currentTimeMillis() + ".out";
        properties.put("outfile", outfile);
        return outfile;
    }

}
