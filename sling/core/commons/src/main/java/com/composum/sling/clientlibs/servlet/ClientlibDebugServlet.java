package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.*;
import com.composum.sling.clientlibs.handle.Clientlib.Type;
import com.composum.sling.clientlibs.processor.AbstractClientlibVisitor;
import com.composum.sling.clientlibs.service.ClientlibService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.sling.clientlibs.handle.ClientlibRef.PREFIX_CATEGORY;

/**
 * Prints a rough overview over the structure of the client library, incl. dependent or embedded client libraries.
 * URL e.g. http://localhost:9090/bin/cpm/nodes/debug/clientlibstructure.css.html
 *
 * @author Hans-Peter Stoerr
 * @since 10/2017
 */
@Component(label = "Composum Clientlib Webconsole Plugin",
        description = "Delivers some debugging informations about the clientlibs through the Felix Webconsole")
@Service(value = Servlet.class)
@Properties({
        @Property(name = "felix.webconsole.label", value = "clientlibs"),
        @Property(name = "felix.webconsole.title", value = "Composum Client Libraries"),
        @Property(name = "felix.webconsole.category", value = "Status"),
        @Property(name = "sling.servlet.paths", value = "/bin/cpm/nodes/debug/clientlibstructure"),
})
public class ClientlibDebugServlet extends HttpServlet {

    /**
     * Extracts the (single) selector from a URL.
     */
    protected static final Pattern SELECTOR_REGEX = Pattern.compile(".*\\.([^.]+)\\.html");

    /**
     * Request parameter to restrict output to one library
     */
    protected static final String REQUEST_PARAM_LIB = "lib";

    /**
     * Request parameter to check permissions / view clientlibs for one user.
     */
    protected static final String REQUEST_PARAM_IMPERSONATE = "impersonate";

    /**
     * Request parameter that overrides the type selector, when used from the form.
     */
    protected static final String REQUEST_PARAM_TYPE = "type";

    @Reference
    private ClientlibService clientlibService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    protected void printUsage(HttpServletRequest request, PrintWriter writer, String impersonationparam) {
        String url = request.getRequestURL().toString().replaceAll("\\.[^/]+$", "") + ".css.html";
        writer.println("<h2>Usage:</h2>");
        writer.println("Please give the type of the client library as selector and one or more");
        writer.println("client libraries or -categories as parameter lib. Some examples:<ul>");
        printLinkItem(writer, url + "?lib=category:composum.core.console.browser", impersonationparam);
        printLinkItem(writer, url + "?lib=/libs/composum/nodes/console/clientlibs/base", impersonationparam);
        printLinkItem(writer, url + "?lib=composum/nodes/console/clientlibs/base", impersonationparam);
        writer.println("</ul>This prints the files and other included client libraries.");
        writer.println("It does not check for duplicated elements, as the normal rendering process does.</p>");
        writer.println("This prints all libraries of the type given as selector, no selector prints all:<ul>");
        printLinkItem(writer, url, impersonationparam);
        writer.println("</ul>");
        writer.flush();
    }

    protected void printVerification(PrintWriter writer, Type type) {
        String verificationResults = clientlibService.verifyClientlibPermissions(type, true);
        if (StringUtils.isNotBlank(verificationResults)) {
            writer.println("<hr/><h2>Permission warnings:</h2><pre>");
            writer.println(verificationResults);
            writer.println("</pre>");
        }
    }

    protected void printForm(HttpServletRequest request, PrintWriter writer, Type type) {
        writer.println("<form action=\"" + request.getRequestURL() + "\" method=\"get\">");
        writer.println("Type: <select name=\"type\"> <option value=\"\">All</option>");
        for (Type selectType : Type.values()) {
            writer.print("        <option value=\"" + selectType.name() + "\"");
            if (selectType == type) writer.print(" selected ");
            writer.print(">" + selectType.name());
            writer.println("</option>");
        }
        writer.println("</select>");
        writer.print(" Library: <input type=\"text\" name=\"" + REQUEST_PARAM_LIB + "\" value=\"");
        String lib = request.getParameter(REQUEST_PARAM_LIB);
        if (lib != null) writer.print(lib);
        writer.println("\">\n");
        writer.print(" Impersonate:\n <input type=\"text\" name=\"" + REQUEST_PARAM_IMPERSONATE + "\" value=\"");
        String impersonate = request.getParameter(REQUEST_PARAM_IMPERSONATE);
        if (StringUtils.isNotBlank(impersonate)) writer.print(impersonate);
        writer.println("\">");
        writer.println("<input type=\"submit\" value=\"Submit\">\n </form>\n");
    }

    protected void printLinkItem(PrintWriter writer, String url, String impersonationparam) {
        writer.println("<li><a href=\"" + url + impersonationparam + "\">" + url + "</a></li>");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();

        ResourceResolver resolverToClose = null;
        ResourceResolver resolver;
        String impersonation = request.getParameter(REQUEST_PARAM_IMPERSONATE);
        String impersonationParam = StringUtils.isNotBlank(impersonation) ? "&" + REQUEST_PARAM_IMPERSONATE + "=" + impersonation.trim() : "";
        if (request instanceof SlingHttpServletRequest && StringUtils.isBlank(impersonation)) {
            resolver = ((SlingHttpServletRequest) request).getResourceResolver();
        } else {
            try {
                Map authenticationInfo = StringUtils.isNotBlank(impersonation) ?
                        new HashMap(Collections.singletonMap(ResourceResolverFactory.USER_IMPERSONATION, impersonation))
                        : null;
                resolver = resolverFactory.getAdministrativeResourceResolver(authenticationInfo);
                resolverToClose = resolver;
            } catch (LoginException e) {
                writer.println("Could not login as " + impersonation);
                return;
            }
        }

        try {
            response.setContentType("text/html");
            writer.print("<html><body><h1>Rough structure of client libraries");
            if (StringUtils.isNotBlank(impersonation))
                writer.print(" as seen from " + resolver.getUserID());
            writer.println("</h1>");

            Type requestedType = requestedClientlibType(request);
            if (requestedType == null && null == request.getParameter(REQUEST_PARAM_LIB)) {
                printUsage(request, writer, impersonationParam);
            }
            printForm(request, writer, requestedType);

            List<Type> printTypes = requestedType == null ? Arrays.asList(Type.values()) : Collections.singletonList(requestedType);
            printVerification(writer, requestedType);

            for (Type type : printTypes) {
                if (StringUtils.isBlank(request.getParameter(REQUEST_PARAM_LIB)))
                    printAllLibs(request, writer, type, resolver, impersonationParam);
                else for (String lib : request.getParameterValues(REQUEST_PARAM_LIB)) {
                    ClientlibRef ref;
                    if (lib.startsWith(PREFIX_CATEGORY)) {
                        ref = ClientlibRef.forCategory(type, lib.substring(PREFIX_CATEGORY.length()), false, null);
                    } else {
                        ref = new ClientlibRef(type, lib, false, null);
                    }
                    displayClientlibStructure(request, writer, ref, resolver, impersonationParam);
                }
            }

            writer.println("<hr/></body></html>");
        } finally {
            if (null != resolverToClose) resolverToClose.close();
            writer.close();
        }
    }

    protected Type requestedClientlibType(HttpServletRequest request) {
        Type type = null;
        String uri = request.getRequestURI();
        Matcher matcher = SELECTOR_REGEX.matcher(uri);
        if (matcher.matches())
            type = Type.valueOf(matcher.group(1));
        if (StringUtils.isNotBlank(request.getParameter(REQUEST_PARAM_TYPE)))
            type = Type.valueOf(request.getParameter(REQUEST_PARAM_TYPE));
        return type;
    }

    private void printAllLibs(HttpServletRequest request, PrintWriter writer, Type type, ResourceResolver resolver, String impersonationParam) throws
            ServletException, IOException {
        try {
            QueryManager querymanager = resolver.adaptTo(Session.class).getWorkspace()
                    .getQueryManager();
            String statement = "//element(*)[sling:resourceType='composum/nodes/commons/clientlib']/" + type.name() +
                    "/..  order by path";
            NodeIterator clientlibs = querymanager.createQuery(statement, Query.XPATH).execute().getNodes();
            while (clientlibs.hasNext())
                displayClientlibStructure(request, writer,
                        new Clientlib(type, resolver.getResource(clientlibs.nextNode()
                                .getPath())).getRef(), resolver, impersonationParam);

        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    private void displayClientlibStructure(HttpServletRequest request, PrintWriter writer, ClientlibRef ref, ResourceResolver resolver, String impersonationparam)
            throws IOException, ServletException {
        ClientlibElement clientlib = clientlibService.resolve(ref, resolver);
        String normalizedPath = normalizePath(ref, clientlib, resolver);
        StringBuilder categories = new StringBuilder();
        String requestUrl = request.getRequestURL().toString();
        String url = requestUrl.replaceAll("\\.[^/]+$", "") + "." + ref.type.name() + ".html";
        if (clientlib instanceof Clientlib) {
            Clientlib thelib = (Clientlib) clientlib;
            if (thelib.getCategories().isEmpty()) categories.append(" (no categories)");
            else {
                categories.append(" (in categories ");
                for (String cat : thelib.getCategories()) {
                    categories.append("<a href=\"").append(url)
                            .append("?lib=category:").append(cat).append(impersonationparam)
                            .append("\">").append(cat).append("</a>");
                }
                categories.append(")");
            }
        }
        Validate.notNull(clientlib, "Not found: " + ref);
        writer.println("<hr/>");
        writer.println("<h2>Structure of <a href=\"" + url + "?lib=" + normalizedPath + impersonationparam + "\">" +
                ref + "</a>" + categories + "</h2>");
        writer.println("<code>&lt;cpn:clientlib type=\"" + ref.type.name() + "\" path=\"" + normalizedPath +
                "\"/&gt;</code><br/><hr/><pre>");
        try {
            new DebugVisitor(clientlib, clientlibService, resolver, writer).execute();
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
