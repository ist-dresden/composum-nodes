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

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;

import static com.composum.sling.clientlibs.handle.ClientlibRef.PREFIX_CATEGORY;

/**
 * Prints a rough overview over the structure of the client library, incl. dependent or embedded client libraries.
 *
 * @author Hans-Peter Stoerr
 * @since 10/2017
 */
@SlingServlet(
        methods = HttpConstants.METHOD_GET,
        paths = "/bin/cpm/nodes/debug/clientlibstructure"
)
public class ClientlibDebugServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibDebugServlet.class);

    @Reference
    private ClientlibService clientlibService;

    protected void printUsage(PrintWriter writer) {
        writer.println("Please give the type of the client library as extension and one or more");
        writer.println("client libraries or -categories as parameter lib. For example:");
        writer.println("http://localhost:9090/bin/cpm/nodes/debug/clientlibstructure.css?" +
                "lib=category:composum.core.console.browser");
        writer.println("This prints the files and other included client libraries.");
        writer.println("It does not check for duplicated elements, as the normal rendering process does.");
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println("Print rough structure of client libraries");

        String typeString = request.getRequestPathInfo().getExtension();
        if (StringUtils.isBlank(typeString) || null == request.getParameter("lib")) {
            printUsage(writer);
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
            ClientlibElement clientlib = clientlibService.resolve(ref, request.getResourceResolver());
            Validate.notNull(clientlib, "Not found: " + ref);
            writer.println("=================== Structure of " + ref + ": ===================");
            try {
                new DebugVisitor(clientlib, clientlibService, request.getResourceResolver(), writer).execute();
            } catch (RepositoryException e) {
                throw new ServletException(e);
            }
            writer.println();
        }

        writer.println("DONE");
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
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " Not present: " + mode + ref);
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
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " " + clientlib);
            indentation++;
            super.visit(clientlib, mode, parent);
            indentation--;
        }

        @Override
        public void visit(ClientlibResourceFolder folder, VisitorMode mode, ClientlibResourceFolder parent) throws
                IOException, RepositoryException {
            writer.println(StringUtils.repeat(" ", INDENTAMOUNT * indentation) + mode + " " + folder);
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
