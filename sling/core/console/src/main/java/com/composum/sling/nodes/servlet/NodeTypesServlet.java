package com.composum.sling.nodes.servlet;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static com.composum.sling.nodes.servlet.NodeTypesServlet.SERVLET_PATH;

/**
 * A servlet that exports the nodetypes in the format used in nodetypes.cnd.
 * E.g. <code>http://localhost:9090/bin/cpm/nodes/debug/nodetypes?nameregex=cpp%3A.%2A</code>
 */
@SlingServlet(
        paths = SERVLET_PATH,
        methods = {"GET"},
        description = "Composum Show Nodetype Servlet"
)
@Properties(value = {
        @Property(name = "sling.auth.requirements", value = {"+" + SERVLET_PATH})
})
//@Component(service = Servlet.class,
//        property = {
//                Constants.SERVICE_DESCRIPTION + "=",
//                "sling.servlet.paths=/bin/cpm/nodes/debug/nodetypes",
//                "sling.servlet.methods=" + HttpConstants.METHOD_GET
//        })
public class NodeTypesServlet extends SlingSafeMethodsServlet {

    public static final String SERVLET_PATH = "/bin/cpm/nodes/debug/nodetypes";

    /**
     * Request parameter with a regular expression to select the nodetypes to write.
     */
    public static final String PARAM_NODETYPEREGEX = "nameregex";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        Session session = request.getResourceResolver().adaptTo(Session.class);
        Pattern nodetypeSelector = null;
        String nodetypeParam = request.getParameter(PARAM_NODETYPEREGEX);
        if (StringUtils.isNotBlank(nodetypeParam)) {
            nodetypeSelector = Pattern.compile(nodetypeParam);
        }
        try (PrintWriter writer = response.getWriter()) {
            writeNodetypes(session, writer, nodetypeSelector);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    protected void writeNodetypes(Session session, PrintWriter writer, Pattern nodetypeSelector) throws RepositoryException, IOException {
        final CompactNodeTypeDefWriter cnd = new CompactNodeTypeDefWriter(writer, session, true);
        final List<NodeTypeDefinition> nodetypes = IteratorUtils.toList(session.getWorkspace().getNodeTypeManager().getAllNodeTypes());
        Collections.sort(nodetypes, Comparator.comparing(NodeTypeDefinition::getName));
        for (NodeTypeDefinition definition : nodetypes) {
            if (nodetypeSelector != null && !nodetypeSelector.matcher(definition.getName()).matches()) {
                continue;
            }
            cnd.write(definition);
        }
        cnd.close();
    }

}
