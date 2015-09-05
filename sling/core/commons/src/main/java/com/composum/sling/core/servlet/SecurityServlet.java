package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.NamedAccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The service servlet to retrieve all general system settings.
 */
@SlingServlet(
        paths = "/bin/core/security",
        methods = {"GET", "PUT", "DELETE"}
)
public class SecurityServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityServlet.class);

    public static final String PARAM_SCOPE = "scope";

    public enum PolicyScope {local, effective}

    @Reference
    private CoreConfiguration coreConfig;

    //
    // Servlet operations
    //

    public enum Extension {json, html}

    public enum Operation {accessPolicy, accessPolicies, allPolicies}

    protected ServletOperationSet operations = new ServletOperationSet(Extension.json);

    protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    /**
     * setup of the servlet operation set for this servlet instance
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.accessPolicies, new GetAccessPolicies());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.allPolicies, new GetAllAccessPolicies());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html,
                Operation.allPolicies, new GetHtmlAccessRules());

        // PUT
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.accessPolicy, new PutAccessPolicy());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.accessPolicy, new RemoveAccessPolicy());
    }

    public class AccessPolicyEntry {

        public String principal;
        public String path;
        public boolean allow;
        public String[] privileges;
        public String[] restrictions; // 'name=pattern' as created in get operation - for delete
        public String[] restrictionName; // restriction names from entry form - for add
        public String[] restrictionPattern; // restriction patterns from entry form - for add
    }

    //
    // Access Control
    //

    /**
     * create a new AccessControlEntry
     */
    public class PutAccessPolicy implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            try {
                ResourceResolver resolver = request.getResourceResolver();
                Session session = resolver.adaptTo(Session.class);
                AccessControlManager acManager = session.getAccessControlManager();

                String path = AbstractServiceServlet.getPath(request);
                AccessPolicyEntry entry = getJsonObject(request, AccessPolicyEntry.class);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    /**
     * remove an AccessControlEntry
     */
    public class RemoveAccessPolicy implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            try {
                ResourceResolver resolver = request.getResourceResolver();
                Session session = resolver.adaptTo(Session.class);
                AccessControlManager acManager = session.getAccessControlManager();

                String path = AbstractServiceServlet.getPath(request);
                AccessPolicyEntry[] entries = getJsonObject(request, AccessPolicyEntry[].class);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    /**
     * the access rules retrieval for an JSON result of on policy list
     */
    public class GetAccessPolicies implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            try {
                ResourceResolver resolver = request.getResourceResolver();
                Session session = resolver.adaptTo(Session.class);
                AccessControlManager acManager = session.getAccessControlManager();

                String path = AbstractServiceServlet.getPath(request);
                PolicyScope scope = RequestUtil.getParameter(request, PARAM_SCOPE,
                        RequestUtil.getSelector(request, PolicyScope.local));

                AccessControlPolicy[] policies;
                switch (scope) {
                    case effective:
                        policies = acManager.getEffectivePolicies(path);
                        break;
                    default:
                        policies = acManager.getPolicies(path);
                        break;
                }

                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                writePolicies(jsonWriter, policies);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }

        protected void writePolicies(JsonWriter writer, AccessControlPolicy[] policies)
                throws IOException, RepositoryException {
            writer.beginArray();
            for (AccessControlPolicy policy : policies) {
                writePolicy(writer, policy);
            }
            writer.endArray();
        }

        protected void writePolicies(JsonWriter writer, AccessControlPolicyIterator policies)
                throws IOException, RepositoryException {
            writer.beginArray();
            while (policies.hasNext()) {
                writePolicy(writer, policies.nextAccessControlPolicy());
            }
            writer.endArray();
        }

        protected void writePolicy(JsonWriter writer, AccessControlPolicy policy)
                throws IOException, RepositoryException {
            if (policy instanceof JackrabbitAccessControlList) {
                JackrabbitAccessControlList acl = (JackrabbitAccessControlList) policy;
                for (AccessControlEntry entry : acl.getAccessControlEntries()) {
                    JackrabbitAccessControlEntry jrEntry = (JackrabbitAccessControlEntry) entry;
                    writer.beginObject();
                    writer.name("principal").value(entry.getPrincipal().getName());
                    writer.name("path").value(acl.getPath());
                    writer.name("allow").value(jrEntry.isAllow());
                    writePrivileges(writer, entry);
                    writeRestrictions(writer, jrEntry);
                    writer.endObject();
                }
            }
        }

        protected void writePrivileges(JsonWriter writer, AccessControlEntry entry)
                throws IOException, RepositoryException {
            Privilege[] privileges = entry.getPrivileges();
            writer.name("privileges");
            writePrivileges(writer, privileges);
        }

        protected void writePrivileges(JsonWriter writer, Privilege[] privileges)
                throws IOException, RepositoryException {
            writer.beginArray();
            for (Privilege privilege : privileges) {
                writer.value(privilege.getName());
            }
            writer.endArray();
        }

        protected void writeRestrictions(JsonWriter writer, JackrabbitAccessControlEntry entry)
                throws IOException, RepositoryException {
            String[] restrictionNames = entry.getRestrictionNames();
            writer.name("restrictions").beginArray();
            for (String name : restrictionNames) {
                writer.value(name + "=" + entry.getRestriction(name).getString());
            }
            writer.endArray();
        }
    }

    /**
     * the access rules retrieval for an JSON result of all policy aspects
     */
    public class GetAllAccessPolicies extends GetAccessPolicies {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            try {
                ResourceResolver resolver = request.getResourceResolver();
                Session session = resolver.adaptTo(Session.class);
                AccessControlManager acManager = session.getAccessControlManager();

                String path = AbstractServiceServlet.getPath(request);

                AccessControlPolicy[] policies;
                AccessControlPolicyIterator iterator;
                Privilege[] privileges;

                JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                jsonWriter.setIndent("  ");
                jsonWriter.beginObject();

                jsonWriter.name("policies");
                policies = acManager.getPolicies(path);
                writePolicies(jsonWriter, policies);

                jsonWriter.name("effective");
                policies = acManager.getEffectivePolicies(path);
                writePolicies(jsonWriter, policies);

                jsonWriter.name("applicable");
                iterator = acManager.getApplicablePolicies(path);
                writePolicies(jsonWriter, iterator);

                jsonWriter.name("privileges");
                privileges = acManager.getPrivileges(path);
                writePrivileges(jsonWriter, privileges);

                jsonWriter.name("supported");
                privileges = acManager.getSupportedPrivileges(path);
                writePrivileges(jsonWriter, privileges);

                jsonWriter.endObject();

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    /**
     * the access rules retrieval for an HTML table result
     */
    public class GetHtmlAccessRules implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws ServletException, IOException {

            try {
                ResourceResolver resolver = request.getResourceResolver();
                Session session = resolver.adaptTo(Session.class);
                AccessControlManager acManager = session.getAccessControlManager();

                String path = AbstractServiceServlet.getPath(request);

                AccessControlPolicy[] policies;

                PrintWriter writer = response.getWriter();
                writer.append("<tbody>");

                writer.append("<tr class=\"policies info\"><th colspan=\"5\">node policies</th></tr>");
                policies = acManager.getPolicies(path);
                writePolicies(writer, policies, "policies");

                writer.append("<tr class=\"effective info\"><th colspan=\"5\">effective policies</th></tr>");
                policies = acManager.getEffectivePolicies(path);
                writePolicies(writer, policies, "effective");

                writer.append("</tbody>");

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }

        protected void writePolicies(PrintWriter writer, AccessControlPolicy[] policies, String rowCss)
                throws IOException, RepositoryException {
            if (policies.length > 0) {
                for (AccessControlPolicy policy : policies) {
                    writePolicy(writer, policy, rowCss);
                }
            } else {
                writer.append("<tr class=\"empty\"><td colspan=\"5\">no rules found</td></tr>");
            }
        }

        protected void writePolicies(PrintWriter writer, AccessControlPolicyIterator policies, String rowCss)
                throws IOException, RepositoryException {
            if (policies.hasNext()) {
                while (policies.hasNext()) {
                    writePolicy(writer, policies.nextAccessControlPolicy(), rowCss);
                }
            } else {
                writer.append("<tr class=\"empty\"><td colspan=\"5\">no rules found</td></tr>");
            }
        }

        protected void writePolicy(PrintWriter writer, AccessControlPolicy policy, String rowCss)
                throws IOException, RepositoryException {
            if (policy instanceof JackrabbitAccessControlList) {
                JackrabbitAccessControlList acl = (JackrabbitAccessControlList) policy;
                for (AccessControlEntry entry : acl.getAccessControlEntries()) {
                    JackrabbitAccessControlEntry jrEntry = (JackrabbitAccessControlEntry) entry;
                    writer.append("<tr class=\"").append(rowCss).append("\">");
                    writer.append("<td class=\"principal\">").append(entry.getPrincipal().getName()).append("</td>");
                    writer.append("<td class=\"path\">").append(acl.getPath()).append("</td>");
                    writer.append("<td class=\"type ")
                            .append(jrEntry.isAllow() ? "allow" : "deny")
                            .append("\">")
                            .append(jrEntry.isAllow() ? "allow" : "deny")
                            .append("</td>");
                    writer.append("<td class=\"privileges\">");
                    writePrivileges(writer, entry);
                    writer.append("</td>");
                    writer.append("<td class=\"restrictions\">");
                    writeRestrictions(writer, jrEntry);
                    writer.append("</td>");
                }
                writer.append("</tr>");
            } else if (policy instanceof AccessControlList) {
                AccessControlList acl = (AccessControlList) policy;
                for (AccessControlEntry entry : acl.getAccessControlEntries()) {
                    writer.append("<tr class=\"").append(rowCss).append("\">");
                    writer.append("<td class=\"principal\">").append(entry.getPrincipal().getName()).append("</td>");
                    writer.append("<td class=\"path\">").append("").append("</td>");
                    writer.append("<td class=\"type\">").append("</td>");
                    writer.append("<td class=\"privileges\">");
                    writePrivileges(writer, entry);
                    writer.append("</td>");
                    writer.append("<td class=\"restrictions\">");
                    writer.append("</td>");
                }
                writer.append("</tr>");
            } else if (policy instanceof NamedAccessControlPolicy) {
                NamedAccessControlPolicy namedPolicy = (NamedAccessControlPolicy) policy;
                writer.append("<tr class=\"named warning\"><td colspan=\"5\">named policy: ")
                        .append(namedPolicy.getName())
                        .append("</td></tr>");
            } else {
                writer.append("<tr class=\"unknown warning\"><td colspan=\"5\">uknown policy type: ")
                        .append(policy.getClass().getName())
                        .append("</td></tr>");
            }
        }

        protected void writePrivileges(PrintWriter writer, AccessControlEntry entry)
                throws IOException, RepositoryException {
            Privilege[] privileges = entry.getPrivileges();
            for (int i = 0; i < privileges.length; ) {
                writer.append(privileges[i].getName());
                if (++i < privileges.length) {
                    writer.append(", ");
                }
            }
        }

        protected void writeRestrictions(PrintWriter writer, JackrabbitAccessControlEntry entry)
                throws IOException, RepositoryException {
            String[] restrictionNames = entry.getRestrictionNames();
            for (int i = 0; i < restrictionNames.length; ) {
                writer.append(restrictionNames[i])
                        .append("=")
                        .append(entry.getRestriction(restrictionNames[i]).getString());
                if (++i < restrictionNames.length) {
                    writer.append(", ");
                }
            }
        }
    }
}