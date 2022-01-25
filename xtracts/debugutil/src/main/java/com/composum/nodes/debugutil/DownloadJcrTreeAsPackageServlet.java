package com.composum.nodes.debugutil;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * This is a servlet that enables to download a package of a JCR subtree without
 * having to create a package in the package manager. The package is
 * created on the fly, and not saved to disk / the repository, so this is
 * better suited to large content trees, but valuable also for quickly downloading
 * a page.
 * <p>
 * CAUTION: not suitable for production, only for internal testing systems!
 * <p>
 * Usage with curl e.g.
 * curl -s -S -o tmp.zip -u admin:admin http://localhost:6502/bin/cpm/nodes/debug/downloadjcr.html/{path to download}
 * <p>
 * Example Creates a package of /some/path:
 * http://localhost:6502/bin/cpm/nodes/debug/downloadjcr.html/some/path
 * <p>
 * Fragment of a bash script to download a path from another AEM system and install it locally on AEM:
 * path=$1
 * TMPFIL=`mktemp -u`.zip
 * trap "{ rm -f $TMPFIL; }" EXIT
 * PKG=$(basename $TMPFIL)
 * curl -S -o $TMPFIL -u ${OTHER_USER}:${OTHER_PWD} http://${OTHERIP}:4502/bin/cpm/nodes/debug/downloadjcr.html${path}?packageName=$PKG
 * curl -u admin:admin -F file=@"$TMPFIL" -F name="tmp $path" -F force=true -F install=true http://localhost:6502/crx/packmgr/service.jsp
 * curl -u admin:admin -F cmd=delete http://localhost:6502/crx/packmgr/service/.json/etc/packages/my_packages/${PKG%%.zip}-1.zip
 * <p>
 *
 * @see "https://gist.github.com/stoerr/a54c58b4b05770c6a7ee39654ea84305"
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil Download Jcr Tree Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/nodes/debug/downloadjcr",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = DownloadJcrTreeAsPackageServlet.Configuration.class)
public class DownloadJcrTreeAsPackageServlet extends SlingSafeMethodsServlet {

    /**
     * Parameter that specifies the name of the package; default is automatically generated from the path.
     */
    private static final String PARAM_PACKAGENAME = "packageName";

    /**
     * Parameter that sets the import mode of the package, default: {@link ImportMode#REPLACE} .
     */
    private static final String PARAM_MERGE = "merge";

    private volatile Configuration config;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (config == null || !config.enabled()) {
            super.doGet(request, response);
            return;
        }

        String userID = request.getResourceResolver().getUserID();
        if (userID == null || !Pattern.compile(config.allowedUserRegex()).matcher(userID).matches()) {
            throw new IllegalArgumentException("Not allowed for user " + userID);
        }

        RequestPathInfo pathInfo = request.getRequestPathInfo();
        String resourcePath = Objects.requireNonNull(pathInfo.getSuffix(), "Suffix required to determine resource.");
        ResourceResolver resolver = request.getResourceResolver();
        Resource resource = resolver.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("Not found: " + resourcePath);
        }
        if (!resource.getPath().matches("/.*/.*/.*")) {
            throw new IllegalArgumentException("Path too short - package would probably be too large");
        }
        if (!"admin".equals(request.getResourceResolver().getUserID())) {
            throw new IllegalArgumentException("For security reasons only allowed for admin");
        }
        resourcePath = resource.getPath();

        String packageName = "tmp-" + resourcePath.replaceAll("[^a-zA-Z0-9_-]+", "_");
        if (StringUtils.isNotBlank(request.getParameter(PARAM_PACKAGENAME))) {
            packageName = request.getParameter(PARAM_PACKAGENAME);
        }

        String mergeParam = request.getParameter(PARAM_MERGE);
        ImportMode importMode = StringUtils.isNotBlank(mergeParam) ? ImportMode.valueOf(mergeParam) : null;

        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", "attachment; filename=" +
                packageName + ".zip");

        BundleContext bundleContext = FrameworkUtil.getBundle(Packaging.class).getBundleContext();
        ServiceReference<Packaging> packagingRef = bundleContext.getServiceReference(Packaging.class);
        Packaging packaging = bundleContext.getService(packagingRef);
        try {
            writePackage(resource, packageName, request, packaging, importMode, response.getOutputStream());
        } catch (RepositoryException | PackageException e) {
            throw new ServletException(e);
        } finally {
            bundleContext.ungetService(packagingRef);
            resolver.revert();
        }
    }

    private void writePackage(Resource resource, String packageName, SlingHttpServletRequest request, Packaging packaging,
                              ImportMode importMode, ServletOutputStream outputStream) throws IOException, RepositoryException, PackageException {

        JcrPackageManager pkgmgr = packaging.getPackageManager(request.getResourceResolver().adaptTo(Session.class));

        JcrPackage jcrPackage = pkgmgr.create("my_packages", packageName, "1");
        try {
            JcrPackageDefinition pkgdef = Objects.requireNonNull(jcrPackage.getDefinition());
            DefaultWorkspaceFilter workspaceFilter = new DefaultWorkspaceFilter();
            workspaceFilter.add(new PathFilterSet(resource.getPath()));
            if (importMode != null) {
                workspaceFilter.setImportMode(importMode);
            }
            pkgdef.setFilter(workspaceFilter, false);

            pkgmgr.assemble(pkgdef, null, outputStream);
        } finally {
            try {
                pkgmgr.remove(jcrPackage);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Activate
    @Modified
    protected void activate(Configuration config) {
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil Download Jcr Tree Servlet",
            description = " This is a servlet that enables to download a package of a JCR subtree without\n" +
                    " having to create a package in the package manager. The package is\n" +
                    " created on the fly, and not saved to disk / the repository, so this is\n" +
                    " better suited to large content trees, but valuable also for quickly downloading\n" +
                    " a page.\n" +
                    " \n" +
                    " CAUTION: not suitable for production, only for internal testing systems!\n" +
                    " \n" +
                    " Usage with curl e.g.\n" +
                    " curl -s -S -o tmp.zip -u admin:admin http://localhost:6502/bin/cpm/nodes/debug/downloadjcr.html/{path to download}\n"
    )
    public @interface Configuration {
        @AttributeDefinition(
                description = "Enable the servlet"
        )
        boolean enabled() default false;

        @AttributeDefinition(
                description = "Regex for allowed users, matching the complete username"
        )
        String allowedUserRegex() default "admin";
    }

}
