package com.composum.sling.nodes.consoleplugin;

import com.composum.sling.core.util.XSS;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefWriter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Displays the stacktraces of active or all threads. Use as console plugin:
 * http://localhost:9090/system/console/nodetype
 */
@Component(label = "Composum Webconsole Nodetype Plugin",
        description = "permits nodetype export")
@Service(value = Servlet.class)
@Properties({
        @Property(name = "felix.webconsole.label", value = "nodetypes"),
        @Property(name = "felix.webconsole.title", value = "Nodetypes"),
        @Property(name = "felix.webconsole.category", value = "Composum"),
        @Property(name = "felix.webconsole.css", value = "nodetypes/" + NodetypesPlugin.LOC_CSS),
})
public class NodetypesPlugin extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(NodetypesPlugin.class);

    /**
     * Location for the CSS.
     */
    protected static final String LOC_CSS = "slingconsole/composum/nodes/console/nodetypesplugin.css";

    public static final String PARAM_NAMEREGEX = "nameregex";

    @Reference
    protected ResourceResolverFactory resolverFactory;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        if (request.getRequestURI().endsWith(LOC_CSS)) {
            response.setContentType("text/css");
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("/" + LOC_CSS),
                    response.getOutputStream());
            return;
        }

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter writer = response.getWriter();

        writer.print("<html><body><h2>Nodetypes</h2>");
        new NodetypesRunner(writer, request).print();
        writer.println("</body></html>");
    }

    protected class NodetypesRunner {
        protected final PrintWriter writer;
        protected final HttpServletRequest request;
        protected Pattern nameRegex = null;

        public NodetypesRunner(PrintWriter writer, HttpServletRequest request) {
            this.writer = writer;
            this.request = request;

            if (StringUtils.isNotBlank(XSS.filter(request.getParameter(PARAM_NAMEREGEX)))) {
                String nameRegexStr = XSS.filter(request.getParameter(PARAM_NAMEREGEX));
                try {
                    if (StringUtils.isNotBlank(nameRegexStr)) {
                        nameRegex = Pattern.compile(nameRegexStr);
                    }
                } catch (PatternSyntaxException e) {
                    writer.println("<p><strong>Regex syntax error: " + e + "</strong></p>");
                }
            }
        }

        public void print() throws ServletException {
            printForm();
            printNodetypes();
        }

        protected void printNodetypes() throws ServletException {
            writer.println("<pre>");
            ResourceResolver resolver = null;
            try {
                resolver = resolverFactory.getResourceResolver(null);
                writeNodetypes(resolver.adaptTo(Session.class), writer, nameRegex);
            } catch (LoginException e) {
                LOG.error("Cannot get resolver", e);
                writer.println("Cannot get resolver");
                throw new ServletException("Cannot get resolver", e);
            } catch (RepositoryException | IOException e) {
                LOG.error("" + e, e);
                throw new ServletException(e);
            } finally {
                if (resolver != null) {
                    resolver.close();
                }
            }
            writer.println("</pre>");
        }

        protected void writeNodetypes(Session session, PrintWriter writer, Pattern nodetypeSelector) throws RepositoryException, IOException {
            final CompactNodeTypeDefWriter cnd = new CompactNodeTypeDefWriter(writer, session, true);
            final NodeTypeIterator iter = session.getWorkspace().getNodeTypeManager().getAllNodeTypes();
            while (iter.hasNext()) {
                NodeTypeDefinition definition = iter.nextNodeType();
                if (nodetypeSelector != null && !nodetypeSelector.matcher(definition.getName()).matches()) {
                    continue;
                }
                cnd.write(definition);
            }
            cnd.close();
        }

        protected void printForm() {
            writer.println("<form action=\"" + request.getRequestURL() + "\" method=\"get\">");
            writer.println("Restrict printed nodetypes with name matching regular expression: ");
            writer.println(" <input type=\"text\" name=\"nameregex\" value=\"" +
                    (nameRegex != null ? nameRegex.pattern() : "")
                    + "\">");
            writer.println(" <input type=\"submit\">\n");
            writer.println("</form>");
        }
    }
}
