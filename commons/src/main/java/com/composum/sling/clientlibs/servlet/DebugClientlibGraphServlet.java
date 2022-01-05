package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibElement;
import com.composum.sling.clientlibs.handle.ClientlibFile;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.handle.ClientlibResourceFolder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tries to print a graph of all client libraries. Unfortunately it is somewhat too complex to be useful.
 * <p>
 * Plot the result with viz-js.com or try with graphviz, e.g. with some of the command lines <br/> <code>unflatten -f -l
 * 4 -c 6 input.dot | dot | gvpack -array_t6 | neato -s -n2 -Tpng -o output.png</code> <br/> or <br/> <code>ccomps -x
 * graph.dot | dot | gvpack -array3 | neato -Tpng -n2 -o graph.png</code>
 *
 * @author Hans-Peter Stoerr
 * @see "viz-js.com"
 * @since 10/2017
 * @deprecated Not yet fully functional
 */
@Component(service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + DebugClientlibGraphServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                "sling.auth.requirements=" + DebugClientlibGraphServlet.SERVLET_PATH
        })
@Deprecated
public class DebugClientlibGraphServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DebugClientlibGraphServlet.class);

    public static final String SERVLET_PATH = "/bin/cpm/nodes/debug/clientlibgraph";

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        try {
            new ClientlibGrapher(request, response).run();
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    private class ClientlibGrapher {

        private final PrintWriter w;
        private final SlingHttpServletRequest request;
        private final SlingHttpServletResponse response;
        private final ResourceResolver resolver;
        private final String type;
        private final boolean compressfolders;

        public ClientlibGrapher(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response) throws IOException {
            this.w = response.getWriter();
            this.request = request;
            this.response = response;
            this.resolver = request.getResourceResolver();
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            type = pathInfo.getExtension();
            compressfolders = null == pathInfo.getSelectorString() || !pathInfo.getSelectorString().contains("folders");
        }

        public void run() throws RepositoryException {
            response.setContentType("text/plain");
            QueryManager querymanager = Objects.requireNonNull(resolver.adaptTo(Session.class)).getWorkspace().getQueryManager();
            String statement = "//element(*)[sling:resourceType='composum/nodes/commons/clientlib']/" + type + "/..";
            NodeIterator clientlibs = querymanager.createQuery(statement, Query.XPATH).execute().getNodes();
            List<Clientlib> libs = new ArrayList<>();
            while (clientlibs.hasNext())
                libs.add(new Clientlib(Clientlib.Type.valueOf(type), resolver.getResource(clientlibs.nextNode()
                        .getPath())));
            w.println("digraph \"clientlibs." + type + "\" {");
            for (Clientlib lib : libs) {
                String path = lib.getResourceFolder().resource.getPath();
                for (String cat : lib.getCategories()) {
                    e(cat, path, true);
                }
                ClientlibResourceFolder rf = lib.getResourceFolder();
                w.println("subgraph \"cluster_" + path + "\" {");
                w.println("label=" + q(path) + ";");
                n(path, null);
                w.println(q(path) + ";");
                childedges(rf, rf);
                w.println("}");
                references(rf, libs, rf);
                w.println();
            }
            w.println("}");
        }

        private void references(ClientlibResourceFolder rf, List<Clientlib> libs,
                                ClientlibResourceFolder topfolder) {
            for (ClientlibRef clientlibRef : rf.getEmbedded()) reference(rf, libs, clientlibRef, true, topfolder);
            for (ClientlibRef clientlibRef : rf.getDependencies())
                reference(rf, libs, clientlibRef, false, topfolder);
            for (ClientlibElement clientlibElement : rf.getChildren())
                if (clientlibElement instanceof ClientlibResourceFolder)
                    references((ClientlibResourceFolder) clientlibElement, libs, topfolder);
        }

        private void reference(ClientlibResourceFolder rf, List<Clientlib> libs, ClientlibRef ref, boolean embedded,
                               ClientlibResourceFolder topfolder) {
            String path = compressfolders ? topfolder.resource.getPath() : rf.resource.getPath();
            if (ref.isCategory()) {
                e(path, ref.category, embedded);
                return;
            }
            for (Clientlib lib : libs) {
                if (ref.isSatisfiedby(lib.makeLink())) {
                    e(path, lib.getResourceFolder().resource.getPath(), embedded);
                    return;
                }
            }
            Resource file = resolver.resolve(ref.path);
            //... ???
        }

        private void childedges(ClientlibResourceFolder rf,
                                ClientlibResourceFolder topfolder) {
            String path = compressfolders ? topfolder.resource.getPath() : rf.resource.getPath();
            n(path, rf.resource.getName() + (rf.getExpanded() ? " (E)" : ""));
            for (ClientlibElement clientlibElement : rf.getChildren()) {
                if (clientlibElement instanceof ClientlibResourceFolder) {
                    ClientlibResourceFolder cf = (ClientlibResourceFolder) clientlibElement;
                    if (!compressfolders) e(path, cf.resource.getPath(), true);
                    childedges(cf, topfolder);
                } else if (clientlibElement instanceof ClientlibFile) {
                    ClientlibFile file = (ClientlibFile) clientlibElement;
                    // e(path, file.handle.getPath(), true);
                    // n(file.handle.getPath(), file.handle.getName());
                } else {
                    e(path, "ERROR: Unknown child type " + clientlibElement, true);
                }
            }
        }

        private void e(String n1, String n2, boolean embed) {
            w.println(q(n1) + " -> " + q(n2) + (embed ? "" : " [style=dashed]") + ";");
        }

        private void n(String path, String alias) {
            if (null == alias) w.println(q(path) + ";");
            else w.println(q(path) + "[label=" + q(alias) + "];");
        }

        private String q(String s) {
            return " \"" + s + "\" ";
        }

    }
}
