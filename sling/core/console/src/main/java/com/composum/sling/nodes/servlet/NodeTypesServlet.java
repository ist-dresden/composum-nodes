package com.composum.sling.nodes.servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

/**
 * A servlet that exports the nodetypes in the format used in nodetypes.cnd.
 * E.g. <code>http://localhost:9090/bin/cpm/nodes/debug/nodetypes?nameregex=cpp%3A.%2A</code>
 */
@SlingServlet(
        paths = "/bin/cpm/nodes/debug/nodetypes",
        methods = {"GET"},
        description = "Composum Show Nodetype Servlet"
)
//@Component(service = Servlet.class,
//        property = {
//                Constants.SERVICE_DESCRIPTION + "=",
//                "sling.servlet.paths=/bin/cpm/nodes/debug/nodetypes",
//                "sling.servlet.methods=" + HttpConstants.METHOD_GET
//        })
public class NodeTypesServlet extends SlingSafeMethodsServlet {

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

}
