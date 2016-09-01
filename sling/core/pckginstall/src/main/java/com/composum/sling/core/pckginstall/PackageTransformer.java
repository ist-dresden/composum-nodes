package com.composum.sling.core.pckginstall;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Component
public class PackageTransformer implements ResourceTransformer, InstallTaskFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private BundleContext bundleContext;

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
                        TransformationResult tr = new TransformationResult();
                        tr.setResourceType("package");
                        final Map<String, Object> attr = new HashMap<>();
                        tr.setAttributes(attr);
                        tr.setId(title);
                        Version v = new Version(version.replace('-', '.'));
                        tr.setVersion(v);
                        return new TransformationResult[]{tr};
                    }
                }
            } catch (Exception e) {
                logger.warn("Exception transforming RegisteredResource: " + e.toString());
            }
        }
        return new TransformationResult[0];
    }

    @Override
    public InstallTask createTask(TaskResourceGroup toActivate) {
        if ( !toActivate.getActiveResource().getType().equals("package") ) {
            return null;
        }
        return new PackageInstallTask(toActivate);
    }

    private class PackageInstallTask extends InstallTask {

        PackageInstallTask(TaskResourceGroup r) {
            super(r);
        }

        @Override
        public void execute(InstallationContext ctx) {
            try {
                final ServiceReference<Repository> serviceReference = bundleContext.getServiceReference(Repository.class);
                final Repository repository = bundleContext.getService(serviceReference);
                final Method loginAdministrative = repository.getClass().getMethod("loginAdministrative", String.class);
                final Object invoke = loginAdministrative.invoke(repository, (Object) null);
                Session session = (Session) invoke;
                final JcrPackageManager packageManager = PackagingService.getPackageManager(session);
                final TaskResource resource = getResource();
                final InputStream inputStream = resource.getInputStream();
                final JcrPackage jcrPackage = packageManager.upload(inputStream, true, true);

                final ServiceReference<JobManager> jmRef = bundleContext.getServiceReference(JobManager.class);
                final JobManager jm = bundleContext.getService(jmRef);
                String path = jcrPackage.getNode().getPath();
                String root = packageManager.getPackageRoot().getPath();
                if (path.startsWith(root)) {
                    path = path.substring(root.length());
                }

                Map<String, Object> jobProperties = new HashMap<>();
                jobProperties.put("reference", path);
                jobProperties.put("operation", "install");
                jobProperties.put("userid", session.getUserID());
                buildOutfileName(jobProperties);
                Job job = jm.addJob("com/composum/sling/core/pckgmgr/PackageJobExecutor", jobProperties);
                this.setFinishedState(ResourceState.INSTALLED);
            } catch (Exception e) {
                logger.warn("Exception executing PackageInstallTask: " + e.toString());
            }
        }

        @Override
        public String getSortKey() {
            return "60-" + getResource().getAttribute(Constants.SERVICE_PID);
        }

    }

    /**
     * Read the manifest from supplied input stream, which is closed before return.
     */
    private static Manifest getManifest(final RegisteredResource rsrc)
            throws IOException {
        final InputStream ins = rsrc.getInputStream();

        Manifest result = null;

        if ( ins != null ) {
            JarInputStream jis = null;
            try {
                jis = new JarInputStream(ins);
                result= jis.getManifest();
            } finally {

                // close the jar stream or the inputstream, if the jar
                // stream is set, we don't need to close the input stream
                // since closing the jar stream closes the input stream
                if (jis != null) {
                    try {
                        jis.close();
                    } catch (IOException ignore) {
                    }
                } else {
                    try {
                        ins.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return result;
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
