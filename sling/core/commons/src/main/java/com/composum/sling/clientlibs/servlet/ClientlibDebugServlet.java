package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.*;
import com.composum.sling.clientlibs.processor.AbstractClientlibVisitor;
import com.composum.sling.clientlibs.service.ClientlibService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;

import static com.composum.sling.clientlibs.handle.ClientlibRef.PREFIX_CATEGORY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Prints a rough overview over the structure of the client library, incl. dependent or embedded client libraries.
 * URL e.g. http://localhost:9090/bin/cpm/nodes/debug/clientlibstructure.css.html
 *
 * @author Hans-Peter Stoerr
 * @since 10/2017
 */
@SlingServlet(
        methods = HttpConstants.METHOD_GET,
        paths = ClientlibDebugServlet.PATH
)
public class ClientlibDebugServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibDebugServlet.class);

    public static final String PATH = "/bin/cpm/nodes/debug/clientlibstructure";

    @Reference
    private ClientlibService clientlibService;

    protected void printUsage(SlingHttpServletRequest request, PrintWriter writer) {
        String url = request.getRequestURL().toString().replaceAll("\\.[^/]+$", "") + ".css.html";
        writer.println("<h2>Usage:</h2>");
        writer.println("Please give the type of the client library as selector and one or more");
        writer.println("client libraries or -categories as parameter lib. Some examples:<ul>");
        printLinkItem(writer, url + "?lib=category:composum.core.console.browser");
        printLinkItem(writer, url + "?lib=/libs/composum/nodes/console/clientlibs/base");
        printLinkItem(writer, url + "?lib=composum/nodes/console/clientlibs/base");
        writer.println("</ul>This prints the files and other included client libraries.");
        writer.println("It does not check for duplicated elements, as the normal rendering process does.</p>");
        writer.println("This prints all libraries of the type given as selector, selector <code>all</code> prints all:<ul>");
        printLinkItem(writer, url);
        writer.println("</ul>");
        writer.flush();
    }

    protected void printVerification(SlingHttpServletRequest request, PrintWriter writer) {
        String verificationResults = clientlibService.verifyClientlibPermissions(true);
        if (StringUtils.isNotBlank(verificationResults)) {
            writer.println("<hr/><h2>Permission warnings:</h2><pre>");
            writer.println(verificationResults);
            writer.println("</pre>");
        }
    }

    protected void printLinkItem(PrintWriter writer, String url) {
        writer.println("<li><a href=\"" + url + "\">" + url + "</a></li>");
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html><body><h1>Rough structure of client libraries</h1>");

        String typeString = request.getRequestPathInfo().getSelectorString();
        if (isBlank(typeString) || null == request.getParameter("lib")) {
            printUsage(request, writer);
            printVerification(request, writer);
            if (isNotBlank(typeString)) printAllLibs(request, writer, typeString);
            return;
        }
        Clientlib.Type type = Clientlib.Type.valueOf(typeString);

        for (String lib : request.getParameterValues("lib")) {
            ClientlibRef ref = null;
            if (lib.startsWith(PREFIX_CATEGORY)) {
                ref = ClientlibRef.forCategory(type, lib.substring(PREFIX_CATEGORY.length()), false, null);
            } else {
                ref = new ClientlibRef(type, lib, false, null);
            }
            displayClientlibStructure(request, writer, ref);
        }

        writer.println("<hr/></body></html>");
    }

    private void printAllLibs(SlingHttpServletRequest request, PrintWriter writer, String typeString) throws
            ServletException, IOException {
        if ("all".equalsIgnoreCase(typeString)) {
            for (Clientlib.Type type : Clientlib.Type.values()) {
                printAllLibs(request, writer, type.name());
            }
        } else try {
            ResourceResolver resolver = request.getResourceResolver();
            QueryManager querymanager = resolver.adaptTo(Session.class).getWorkspace()
                    .getQueryManager();
            String statement = "//element(*)[sling:resourceType='composum/nodes/commons/clientlib']/" + typeString +
                    "/..";
            NodeIterator clientlibs = null;
            clientlibs = querymanager.createQuery(statement, Query.XPATH).execute().getNodes();
            while (clientlibs.hasNext())
                displayClientlibStructure(request, writer,
                        new Clientlib(Clientlib.Type.valueOf(typeString), resolver.getResource(clientlibs.nextNode()
                                .getPath())).getRef());

        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    private void displayClientlibStructure(SlingHttpServletRequest request, PrintWriter writer, ClientlibRef ref)
            throws IOException, ServletException {
        ClientlibElement clientlib = clientlibService.resolve(ref, request.getResourceResolver());
        String normalizedPath = normalizePath(ref, clientlib, request.getResourceResolver());
        StringBuilder categories = new StringBuilder();
        String url = request.getRequestURL().toString().replaceAll("\\.[^/]+$", "") + "." + ref.type.name() + ".html";
        if (clientlib instanceof Clientlib) {
            Clientlib thelib = (Clientlib) clientlib;
            if (thelib.getCategories().isEmpty()) categories.append(" (no categories)");
            else {
                categories.append(" (in categories ");
                for (String cat : thelib.getCategories()) {
                    categories.append("<a href=\"").append(url)
                            .append("?lib=category:").append(cat).append("\">").append(cat).append("</a>");
                }
                categories.append(")");
            }
        }
        Validate.notNull(clientlib, "Not found: " + ref);
        writer.println("<hr/>");
        writer.println("<h2>Structure of <a href=\"" + url + "?lib=" + normalizedPath + "\">" +
                ref + "</a>" + categories + "</h2>");
        writer.println("<code>&lt;cpn:clientlib type=\"" + ref.type.name() + "\" path=\"" + normalizedPath +
                "\"/&gt;</code><br/><hr/><pre>");
        try {
            new DebugVisitor(clientlib, clientlibService, request.getResourceResolver(), writer).execute();
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
        writer.println("</pre>");
    }

    private String normalizePath(ClientlibRef ref, ClientlibElement clientlib, ResourceResolver resolver) {
        if (ref.isCategory()) return "category:" + ref.category;
        if (!ref.path.startsWith("/")) return ref.path;
        for (String pathelement : resolver.getSearchPath()) {
            if (ref.path.startsWith(pathelement)) {
                String normalizedPath = ref.path.substring(pathelement.length());
                // check whether clientlib is shadowed by other lib in other segments of the search path
                ClientlibElement lib2 = clientlibService.resolve(new ClientlibRef(ref.type, normalizedPath, ref
                        .optional, ref.properties), resolver);
                if (lib2 instanceof Clientlib) {
                    Clientlib reresolvedLib = (Clientlib) lib2;
                    if (reresolvedLib.getRef().path.equals(clientlib.getRef().path)) return normalizedPath;
                }
            }
        }
        return ref.path;
    }

    protected class DebugVisitor extends AbstractClientlibVisitor {
        protected static final int INDENTAMOUNT = 8;
        protected int indentation;

        protected final PrintWriter writer;

        protected DebugVisitor(ClientlibElement owner, ClientlibService service, ResourceResolver resolver,
                               PrintWriter writer) {
            super(owner, service, resolver, new LinkedHashSet<ClientlibLink>());
            this.writer = writer;
        }

        @Override
        protected ClientlibVisitor createVisitorFor(ClientlibElement element) {
            return this;
        }

        @Override
        protected boolean isNotProcessed(ClientlibRef ref) {
            return true;
        }

        @Override
        protected void markAsProcessed(ClientlibLink link, ClientlibResourceFolder parent, VisitorMode visitorMode) {
            return;
        }

        @Override
        protected void notPresent(ClientlibRef ref, VisitorMode mode, ClientlibResourceFolder folder) {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " Not present: " + ref);
        }

        @Override
        public void visit(ClientlibCategory category, VisitorMode mode, ClientlibResourceFolder parent) throws
                IOException, RepositoryException {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " " + category);
            indentation++;
            super.visit(category, mode, parent);
            indentation--;
        }

        @Override
        public void visit(Clientlib clientlib, VisitorMode mode, ClientlibResourceFolder parent) throws IOException,
                RepositoryException {
            String order = 0 != clientlib.getOrder() ? " [order=" + clientlib.getOrder() + "]" : "";
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " " + clientlib
                    + order);
            indentation++;
            super.visit(clientlib, mode, parent);
            indentation--;
        }

        @Override
        public void visit(ClientlibResourceFolder folder, VisitorMode mode, ClientlibResourceFolder parent) throws
                IOException, RepositoryException {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " " + folder
                    + (folder.getOptional() ? "[Opt]" : ""));
            indentation++;
            super.visit(folder, mode, parent);
            indentation--;
        }

        @Override
        public void visit(ClientlibFile file, VisitorMode mode, ClientlibResourceFolder parent) throws
                RepositoryException, IOException {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " " + file);
            indentation++;
            super.visit(file, mode, parent);
            indentation--;
        }

        @Override
        public void visit(ClientlibExternalUri externalUri, VisitorMode mode, ClientlibResourceFolder parent) {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " " + externalUri);
            indentation++;
            super.visit(externalUri, mode, parent);
            indentation--;
        }

    }

}
