package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.Clientlib.Type;
import com.composum.sling.clientlibs.handle.ClientlibCategory;
import com.composum.sling.clientlibs.handle.ClientlibElement;
import com.composum.sling.clientlibs.handle.ClientlibExternalUri;
import com.composum.sling.clientlibs.handle.ClientlibFile;
import com.composum.sling.clientlibs.handle.ClientlibLink;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.handle.ClientlibResourceFolder;
import com.composum.sling.clientlibs.handle.ClientlibVisitor;
import com.composum.sling.clientlibs.processor.AbstractClientlibVisitor;
import com.composum.sling.clientlibs.service.ClientlibService;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import javax.annotation.Nonnull;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.composum.sling.clientlibs.handle.ClientlibRef.PREFIX_CATEGORY;

/**
 * Prints a rough overview over the structure of the client library, incl. dependent or embedded client libraries.
 * Works as a Sling Console plugin.
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
        @Property(name = "felix.webconsole.category", value = "Composum"),
        @Property(name = "felix.webconsole.css", value = "clientlibs/" + ClientlibDebugConsolePlugin.LOC_CSS),
})
public class ClientlibDebugConsolePlugin extends HttpServlet {

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

    /**
     * Request parameter that triggers clearing the clientlib cache. Obviously something to be used sparingly.
     */
    protected static final String REQUEST_PARAM_CLEAR_CACHE = "clearCache";

    /** Location for the CSS. */
    protected static final String LOC_CSS = "slingconsole/composumplugin.css";

    @Reference
    protected ClientlibService clientlibService;

    @Reference
    protected ResourceResolverFactory resolverFactory;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().endsWith(LOC_CSS)) {
            response.setContentType("text/css");
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("/" + LOC_CSS),
                    response.getOutputStream());
            return;
        }

        String impersonation = request.getParameter(REQUEST_PARAM_IMPERSONATE);
        PrintWriter writer = response.getWriter();
        final Processor processor = new Processor(request, impersonation, writer);
        try {
            initResolvers(processor, impersonation);

            if (StringUtils.isNotBlank(request.getParameter(REQUEST_PARAM_CLEAR_CACHE))) {
                clientlibService.clearCache(processor.adminResolver);
                String location = request.getRequestURI() + "?" + request.getQueryString().replaceAll(REQUEST_PARAM_CLEAR_CACHE, REQUEST_PARAM_CLEAR_CACHE + "Done");
                processor.adminResolver.commit();
                response.sendRedirect(location);
                return;
            }
            if (StringUtils.isNotBlank(request.getParameter(REQUEST_PARAM_CLEAR_CACHE + "Done"))) {
                writer.println("<br><br><br><h3>The complete clientlib cache was cleared.</h3><br>");
            }

            response.setContentType("text/html"); // XSS? - checked (2019-05-04)

            writer.print("<html><body><h2>Structure of client libraries");
            if (StringUtils.isNotBlank(impersonation))
                writer.print(" as seen from " + processor.impersonationResolver.getUserID());
            writer.println("</h2>");

            processor.requestedType = requestedClientlibType(request);
            if (processor.requestedType == null && null == request.getParameter(REQUEST_PARAM_LIB)) {
                processor.printUsage();
            }
            processor.printForm();
            processor.printVerification();

            List<Type> printTypes = processor.requestedType == null ? Arrays.asList(Type.values()) : Collections.singletonList(processor.requestedType);
            for (Type type : printTypes) {
                processor.type = type;
                if (StringUtils.isBlank(request.getParameter(REQUEST_PARAM_LIB)))
                    processor.printAllLibs();
                else for (String lib : request.getParameterValues(REQUEST_PARAM_LIB)) {
                    ClientlibRef ref;
                    if (lib.startsWith(PREFIX_CATEGORY)) {
                        ref = ClientlibRef.forCategory(type, lib.substring(PREFIX_CATEGORY.length()), false, null);
                    } else {
                        ref = new ClientlibRef(type, lib, false, null);
                    }
                    processor.displayClientlibStructure(ref);
                }
            }

            writer.println("<hr/></body></html>");
        } finally {
            if (null != processor.adminResolver) processor.adminResolver.close();
            if (null != processor.impersonationResolver) processor.impersonationResolver.close();
            writer.close();
        }
    }

    protected void initResolvers(Processor processor, String impersonation) throws ServletException {
        try {
            processor.adminResolver = resolverFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            processor.writer.println("Cannot get administrative resolver");
            throw new ServletException("Cannot get administrative resolver");
        }
        if (StringUtils.isBlank(impersonation)) {
            try {
                processor.impersonationResolver = resolverFactory.getResourceResolver(null);
            } catch (LoginException e) { // impossible - so we want to know about it.
                processor.writer.println("Cannot get anonymous resolver");
                throw new ServletException("Cannot get anonymous resolver");
            }
        } else {
            try {
                Map authenticationInfo = StringUtils.isNotBlank(impersonation) ?
                        new HashMap(Collections.singletonMap(ResourceResolverFactory.USER_IMPERSONATION, impersonation))
                        : null;
                processor.impersonationResolver = resolverFactory.getAdministrativeResourceResolver(authenticationInfo);
            } catch (LoginException e) {
                processor.writer.println("Could not login as " + impersonation);
                throw new ServletException("Could not login as " + impersonation);
            }
        }
    }

    protected Type requestedClientlibType(HttpServletRequest request) {
        Type type = null;
        String uri = request.getRequestURI();
        Matcher matcher = SELECTOR_REGEX.matcher(uri);
        if (matcher.matches())
            type = Type.valueOf(matcher.group(1));
        String typeParameter = request.getParameter(REQUEST_PARAM_TYPE);
        if (StringUtils.isNotBlank(typeParameter)) {
            type = "all".equalsIgnoreCase(typeParameter) ? null : Type.valueOf(typeParameter);
        }
        return type;
    }

    /** Encapsulates stuff always needed during one request, to avoid passing it on through all methods as parameters. */
    protected class Processor {
        Type requestedType;
        Type type;
        HttpServletRequest request;
        PrintWriter writer;
        ResourceResolver adminResolver;
        ResourceResolver impersonationResolver;
        String impersonation;
        String impersonationParam;

        public Processor(HttpServletRequest request, String impersonation, PrintWriter writer) {
            this.request = request;
            this.impersonation = impersonation;
            this.impersonationParam = StringUtils.isNotBlank(impersonation) ? "&" + REQUEST_PARAM_IMPERSONATE + "=" + impersonation.trim() : "";
            this.writer = writer;
        }

        protected void printUsage() {
            String url = request.getRequestURL().toString().replaceAll("\\.[^/]+$", "") + ".css.html";
            writer.println("<h3>Usage:</h3>");
            writer.println("Please give the type of the client library as selector and one or more");
            writer.println("client libraries or -categories as parameter lib. Some examples:<ul>");
            printLinkItem(url + "?lib=category:composum.nodes.console.browser");
            printLinkItem(url + "?lib=/libs/composum/nodes/console/clientlibs/base");
            printLinkItem(url + "?lib=composum/nodes/console/clientlibs/base");
            writer.println("</ul>This prints the files and other included client libraries.");
            writer.println("It does not check for duplicated elements, as the normal rendering process does.</p>");
            writer.println("This prints all libraries of the type given as selector, no selector prints all:<ul>");
            printLinkItem(url);
            writer.println("</ul>");
            writer.flush();
        }

        protected void printLinkItem(String url) {
            writer.println("<li><a href=\"" + url + impersonationParam + "\">" + url + "</a></li>");
        }

        protected void printForm() {
            writer.println("<form action=\"" + request.getRequestURL() + "\" method=\"get\">");
            writer.println("Type: <select name=\"type\"> <option value=\"all\">All</option>");
            for (Type selectType : Type.values()) {
                writer.print("        <option value=\"" + selectType.name() + "\"");
                if (selectType == requestedType) writer.print(" selected ");
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
            writer.println("<input type=\"submit\" value=\"Submit\">\n");
            writer.println("<br><br><input type=\"submit\" title=\"This shouldn't normally be neccesary - use with caution - all used client libraries have to be rendered again.\" name=\"clearCache\" value=\"Clear the whole Clientlib Cache\">\n");
            writer.println("</form>\n");
        }

        protected void printVerification() {
            String verificationResults = clientlibService.verifyClientlibPermissions(type, impersonationResolver, false);
            if (StringUtils.isNotBlank(verificationResults)) {
                writer.println("<hr/><h3>Permission information / errors for " +
                        StringUtils.defaultIfBlank(impersonation, "anonymous") +
                        ":</h3><pre>");
                writer.println(verificationResults);
                writer.println("</pre>");
            }
        }

        /** Prints all client libraries readable for the impersonation user. */
        protected void printAllLibs() throws ServletException, IOException {
            try {
                QueryManager querymanager = adminResolver.adaptTo(Session.class).getWorkspace()
                        .getQueryManager();
                String statement = "//element(*)[sling:resourceType='composum/nodes/commons/clientlib']/" + type.name() +
                        "/..  order by path";
                List<Resource> clientlibs = IteratorUtils.toList(adminResolver.findResources(statement, Query.XPATH));
                for (Resource clientlibResource : clientlibs) {
                    if (impersonationResolver.getResource(clientlibResource.getPath()) != null)
                        displayClientlibStructure(new Clientlib(type, clientlibResource).getRef());
                }
            } catch (RepositoryException e) { // shouldn't happen
                throw new ServletException(e);
            }
        }

        /** Displays the structure of one clientlib as seen from the adminResolver - it's rendered like that, anyway. */
        protected void displayClientlibStructure(ClientlibRef ref) throws IOException, ServletException {
            ClientlibElement clientlib = clientlibService.resolve(ref, adminResolver);
            String normalizedPath = normalizePath(ref, clientlib);
            StringBuilder categories = new StringBuilder();
            String requestUrl = request.getRequestURL().toString();
            String url = requestUrl.replaceAll("\\.[^/]+$", "") + "." + ref.type.name() + ".html";
            if (clientlib instanceof Clientlib) {
                Clientlib thelib = (Clientlib) clientlib;
                if (thelib.getCategories().isEmpty()) categories.append(" (no categories)");
                else {
                    categories.append(" (in categories ");
                    boolean first = true;
                    for (String cat : thelib.getCategories()) {
                        if (!first) categories.append(", ");
                        first = false;
                        categories.append("<a href=\"").append(url)
                                .append("?lib=category:").append(cat).append(impersonationParam)
                                .append("\">").append(cat).append("</a>");
                    }
                    categories.append(")");
                }
            }
            Validate.notNull(clientlib, "Not found: " + ref);
            writer.println("<hr/>");
            writer.println("<h3>Structure of <a href=\"" + url + "?lib=" + normalizedPath + impersonationParam + "\">" +
                    ref + "</a>" + categories + "</h3>");
            writer.println("<code>&lt;cpn:clientlib type=\"" + ref.type.name() + "\" path=\"" + normalizedPath +
                    "\"/&gt;</code><br/><hr/><pre>");
            try {
                new DebugVisitor(clientlib, clientlibService, adminResolver, writer).execute();
            } catch (RepositoryException e) {
                throw new ServletException(e);
            }
            writer.println("</pre>");
        }

        /** Returns the path as it would be in a clientlib reference: relative to search path or a category link */
        protected String normalizePath(ClientlibRef ref, ClientlibElement clientlib) {
            if (ref.isCategory())
                return "category:" + ref.category;
            if (!ref.path.startsWith("/"))
                return ref.path;
            for (String pathelement : adminResolver.getSearchPath()) {
                if (ref.path.startsWith(pathelement)) {
                    String normalizedPath = ref.path.substring(pathelement.length());
                    // check whether clientlib is shadowed by other lib in other segments of the search path
                    ClientlibElement lib2 = clientlibService.resolve(new ClientlibRef(ref.type, normalizedPath, ref
                            .optional, ref.properties), adminResolver);
                    if (lib2 instanceof Clientlib) {
                        Clientlib reresolvedLib = (Clientlib) lib2;
                        if (reresolvedLib.getRef().path.equals(clientlib.getRef().path))
                            return normalizedPath;
                    }
                }
            }
            return ref.path;
        }

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
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + modeName(mode) + " NOT PRESENT: " + (ref.optional ? "optional " : "mandatory ") + ref);
        }

        @Nonnull
        private String modeName(VisitorMode mode) {
            return VisitorMode.EMBEDDED.equals(mode) ? " embeds " : " depends";
        }

        @Override
        public void visit(ClientlibCategory category, VisitorMode mode, ClientlibResourceFolder parent) throws
                IOException, RepositoryException {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + modeName(mode) + " " + category);
            indentation++;
            super.visit(category, mode, parent);
            indentation--;
        }

        @Override
        public void visit(Clientlib clientlib, VisitorMode mode, ClientlibResourceFolder parent) throws IOException,
                RepositoryException {
            String order = 0 != clientlib.getOrder() ? " [order=" + clientlib.getOrder() + "]" : "";
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + modeName(mode) + " " + clientlib
                    + order);
            indentation++;
            super.visit(clientlib, mode, parent);
            indentation--;
        }

        @Override
        public void visit(ClientlibResourceFolder folder, VisitorMode mode, ClientlibResourceFolder parent) throws
                IOException, RepositoryException {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + modeName(mode) + " " + folder
                    + (folder.getOptional() ? "[Opt]" : ""));
            indentation++;
            super.visit(folder, mode, parent);
            indentation--;
        }

        @Override
        public void visit(ClientlibFile file, VisitorMode mode, ClientlibResourceFolder parent) throws
                RepositoryException, IOException {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + modeName(mode) + " " + file);
            indentation++;
            super.visit(file, mode, parent);
            indentation--;
        }

        @Override
        public void visit(ClientlibExternalUri externalUri, VisitorMode mode, ClientlibResourceFolder parent) {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + modeName(mode) + " " + externalUri);
            indentation++;
            super.visit(externalUri, mode, parent);
            indentation--;
        }

    }

}
