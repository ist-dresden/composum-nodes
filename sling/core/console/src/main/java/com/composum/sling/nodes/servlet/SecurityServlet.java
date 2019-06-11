package com.composum.sling.nodes.servlet;

import com.composum.sling.cpnl.CpnlElFunctions;
import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.util.JsonUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.value.StringValue;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.composum.sling.core.mapping.MappingRules.CHARSET;

/**
 * The service servlet to retrieve all general system settings.
 */
@SlingServlet(
        paths = "/bin/cpm/nodes/security",
        methods = {"GET", "POST", "PUT", "DELETE"}
)
public class SecurityServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityServlet.class);

    public static final String PARAM_SCOPE = "scope";

    public enum PolicyScope {local, effective}

    @Reference
    private NodesConfiguration coreConfig;

    //
    // Servlet operations
    //

    public enum Extension {json, html}

    public enum Operation {accessPolicy, accessPolicies, allPolicies, reorder, supportedPrivileges, principals, restrictionNames}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Override
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
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.supportedPrivileges, new SupportedPrivileges());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.restrictionNames, new RestrictionNames());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.principals, new GetPrincipals());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json,
                Operation.reorder, new ReorderOperation());

        // PUT
        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json,
                Operation.accessPolicy, new PutAccessPolicy());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json,
                Operation.accessPolicy, new RemoveAccessPolicy());
    }

    public static class AccessPolicyEntry {

        public AccessPolicyEntry() {
        }

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

    @SuppressWarnings("Duplicates")
    public class GetPrincipals implements ServletOperation {
        @Override
        public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                         final ResourceHandle resource) throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final String path = AbstractServiceServlet.getPath(request);
                final String name;
                if (path.startsWith("/")) {
                    name = path.substring(1);
                } else {
                    name = path;
                }
                final Query q = new Query() {
                    @Override
                    public <T> void build(final QueryBuilder<T> builder) {
                        builder.setCondition(builder.nameMatches(name + "%"));
                        builder.setSortOrder("@name", QueryBuilder.Direction.ASCENDING);
                        builder.setSelector(Authorizable.class);
                    }
                };
                final Iterator<Authorizable> principals = session.getUserManager().findAuthorizables(q);
                final List<String> principalNames = new ArrayList<>();
                while (principals.hasNext()) {
                    final Authorizable authorizable = principals.next();
                    principalNames.add(authorizable.getPrincipal().getName());
                }
                if ("everyone".startsWith(name)) {
                    principalNames.add("everyone");
                }
                Collections.sort(principalNames);
                final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                response.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.writeJsonArray(jsonWriter, principalNames.iterator());
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    @SuppressWarnings("Duplicates")
    public class RestrictionNames implements ServletOperation {

        @Override
        public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                         final ResourceHandle resource) throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final AccessControlManager acManager = session.getAccessControlManager();
                final String path = AbstractServiceServlet.getPath(request);
                final JackrabbitAccessControlList policy = AccessControlUtils.getAccessControlList(acManager, path);
                String[] restrictionNames = policy.getRestrictionNames();
                Arrays.sort(restrictionNames);
                final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                response.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.writeJsonArray(jsonWriter, restrictionNames);
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    @SuppressWarnings("Duplicates")
    public class SupportedPrivileges implements ServletOperation {

        @Override
        public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                         final ResourceHandle resource) throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final AccessControlManager acManager = session.getAccessControlManager();
                final String path = AbstractServiceServlet.getPath(request);

                final Privilege[] supportedPrivileges = acManager.getSupportedPrivileges(path);
                final List<String> privilegeNames = new ArrayList<>(supportedPrivileges.length);
                for (final Privilege privilege : supportedPrivileges) {
                    privilegeNames.add(privilege.getName());
                }
                Collections.sort(privilegeNames);
                final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response);
                response.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.writeJsonArray(jsonWriter, privilegeNames.iterator());
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    @SuppressWarnings("Duplicates")
    public class ReorderOperation implements ServletOperation {

        @Override
        public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                         final ResourceHandle resource) throws IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final AccessControlManager acManager = session.getAccessControlManager();

                final String path = AbstractServiceServlet.getPath(request);

                final String object = request.getParameter("object");
                final String before = request.getParameter("before");
                final AccessPolicyEntry entryObject = getJsonObject(object, AccessPolicyEntry.class);
                final AccessPolicyEntry entryBefore = getJsonObject(before, AccessPolicyEntry.class);
                final JackrabbitAccessControlList policy = AccessControlUtils.getAccessControlList(acManager, path);
                JackrabbitAccessControlEntry b = null;
                JackrabbitAccessControlEntry o = null;
                for (AccessControlEntry entry : policy.getAccessControlEntries()) {
                    final JackrabbitAccessControlEntry jrEntry = (JackrabbitAccessControlEntry) entry;
                    if (sameEntry(jrEntry, entryBefore)) {
                        b = jrEntry;
                    }
                    if (sameEntry(jrEntry, entryObject)) {
                        o = jrEntry;
                    }
                }
                if (o != null) {
                    policy.orderBefore(o, b);
                    acManager.setPolicy(path, policy);
                    session.save();
                }
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    /**
     * create a new AccessControlEntry
     */
    public class PutAccessPolicy implements ServletOperation {

        @Override
        public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                         final ResourceHandle resource)
                throws ServletException, IOException {

            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final AccessControlManager acManager = session.getAccessControlManager();
                final PrincipalManager principalManager = session.getPrincipalManager();

                final String path = AbstractServiceServlet.getPath(request);
                final AccessPolicyEntry entry = getJsonObject(request, AccessPolicyEntry.class);
                final JackrabbitAccessControlList policy = AccessControlUtils.getAccessControlList(acManager, path);
                final Principal principal = principalManager.getPrincipal(entry.principal);
                final Privilege[] privileges = AccessControlUtils.privilegesFromNames(acManager, entry.privileges);

                final Map<String, Value> restrictions = new HashMap<>();
                for (final String restriction : entry.restrictions) {
                    final Value v = new StringValue(restriction.substring(restriction.indexOf('=') + 1));
                    restrictions.put(restriction.substring(0, restriction.indexOf('=')), v);
                }
                policy.addEntry(principal, privileges, entry.allow, restrictions);
                acManager.setPolicy(path, policy);
                session.save();
            } catch (final RepositoryException ex) {
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
        public void doIt(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                         final ResourceHandle resource) throws ServletException, IOException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final Session session = resolver.adaptTo(Session.class);
                final AccessControlManager acManager = session.getAccessControlManager();

                final String path = AbstractServiceServlet.getPath(request);
                final AccessPolicyEntry[] entries = getJsonObject(request, AccessPolicyEntry[].class);
                final JackrabbitAccessControlList policy = AccessControlUtils.getAccessControlList(acManager, path);
                for (final AccessPolicyEntry entrySendFromClient : entries) {
                    for (final AccessControlEntry entry : policy.getAccessControlEntries()) {
                        final JackrabbitAccessControlEntry jrEntry = (JackrabbitAccessControlEntry) entry;
                        if (sameEntry(jrEntry, entrySendFromClient)) {
                            policy.removeAccessControlEntry(entry);
                        }
                    }
                }
                acManager.setPolicy(path, policy);
                if (policy.isEmpty()) {
                    acManager.removePolicy(path, policy);
                }
                session.save();
            } catch (final RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }

    }

    /**
     * the access rules retrieval for an JSON result of on policy list
     */
    @SuppressWarnings("Duplicates")
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
                //noinspection SwitchStatementWithTooFewBranches
                switch (scope) {
                    case effective:
                        policies = acManager.getEffectivePolicies(path);
                        // two equal sets from the ac manager on root...
                        if ("/".equals(path) && policies.length == 2 && seemsTheSame(policies[0], policies[1])) {
                            policies = new AccessControlPolicy[]{policies[0]};
                        }
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

        protected boolean seemsTheSame(AccessControlPolicy p1, AccessControlPolicy p2) {
            return p1 instanceof JackrabbitAccessControlList &&
                    p2 instanceof JackrabbitAccessControlList &&
                    ((JackrabbitAccessControlList) p1).size() ==
                            ((JackrabbitAccessControlList) p2).size();
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
                throws IOException {
            Privilege[] privileges = entry.getPrivileges();
            writer.name("privileges");
            writePrivileges(writer, privileges);
        }

        protected void writePrivileges(JsonWriter writer, Privilege[] privileges)
                throws IOException {
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
                try {
                    Value value = entry.getRestriction(name);
                    writer.value(name + "=" + (value != null ? value.toString() : "<null>"));
                } catch (Exception ex) {
                    writer.value(name + ":" + ex.toString());
                }
            }
            writer.endArray();
        }
    }

    /**
     * the access rules retrieval for an JSON result of all policy aspects
     */
    @SuppressWarnings("Duplicates")
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
    @SuppressWarnings("Duplicates")
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
                response.setContentType("text/html;charset=" + CHARSET); // XSS? - checked (2019-05-04)

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
                throws RepositoryException {
            if (policies.length > 0) {
                for (AccessControlPolicy policy : policies) {
                    writePolicy(writer, policy, rowCss);
                }
            } else {
                writer.append("<tr class=\"empty\"><td colspan=\"5\">no rules found</td></tr>");
            }
        }

        protected void writePolicies(PrintWriter writer, AccessControlPolicyIterator policies, String rowCss)
                throws RepositoryException {
            if (policies.hasNext()) {
                while (policies.hasNext()) {
                    writePolicy(writer, policies.nextAccessControlPolicy(), rowCss);
                }
            } else {
                writer.append("<tr class=\"empty\"><td colspan=\"5\">no rules found</td></tr>");
            }
        }

        protected void writePolicy(PrintWriter writer, AccessControlPolicy policy, String rowCss)
                throws RepositoryException {
            if (policy instanceof JackrabbitAccessControlList) {
                JackrabbitAccessControlList acl = (JackrabbitAccessControlList) policy;
                for (AccessControlEntry entry : acl.getAccessControlEntries()) {
                    JackrabbitAccessControlEntry jrEntry = (JackrabbitAccessControlEntry) entry;
                    writer.append("<tr class=\"").append(rowCss).append("\">");
                    writer.append("<td class=\"principal\">")
                            .append(CpnlElFunctions.text(entry.getPrincipal().getName())).append("</td>");
                    writer.append("<td class=\"path\">").append(CpnlElFunctions.path(acl.getPath())).append("</td>");
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
                    writer.append("<td class=\"principal\">")
                            .append(CpnlElFunctions.text(entry.getPrincipal().getName())).append("</td>");
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

        protected void writePrivileges(PrintWriter writer, AccessControlEntry entry) {
            Privilege[] privileges = entry.getPrivileges();
            for (int i = 0; i < privileges.length; ) {
                writer.append(privileges[i].getName());
                if (++i < privileges.length) {
                    writer.append(", ");
                }
            }
        }

        protected void writeRestrictions(PrintWriter writer, JackrabbitAccessControlEntry entry)
                throws RepositoryException {
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

    protected boolean sameEntry(final JackrabbitAccessControlEntry jrEntry,
                                final AccessPolicyEntry entrySendFromClient) throws RepositoryException {
        final String p1 = jrEntry.getPrincipal().getName();
        final String p2 = entrySendFromClient.principal;
        final boolean a1 = jrEntry.isAllow();
        final boolean a2 = entrySendFromClient.allow;
        if (p1.equals(p2) && a1 == a2) {
            return samePrivileges(jrEntry, entrySendFromClient) && sameRestrictions(jrEntry, entrySendFromClient);
        }
        return false;
    }

    @SuppressWarnings("BooleanVariableAlwaysNegated")
    protected boolean samePrivileges(final JackrabbitAccessControlEntry jrEntry,
                                     final AccessPolicyEntry entrySendFromClient) {
        if (jrEntry.getPrivileges().length != entrySendFromClient.privileges.length) {
            return false;
        } else {
            for (final Privilege privilegeDefined : jrEntry.getPrivileges()) {
                boolean privilegeFound = false;
                for (final String privilegeFromClient : entrySendFromClient.privileges) {
                    if (privilegeDefined.getName().equals(privilegeFromClient)) {
                        privilegeFound = true;
                        break;
                    }
                }
                if (!privilegeFound) {
                    return false;
                }
            }
            return true;
        }
    }

    @SuppressWarnings("BooleanVariableAlwaysNegated")
    protected boolean sameRestrictions(final JackrabbitAccessControlEntry jrEntry,
                                       final AccessPolicyEntry entrySendFromClient) throws RepositoryException {
        if (jrEntry.getRestrictionNames().length != entrySendFromClient.restrictions.length) {
            return false;
        } else {
            for (final String restrictionName : jrEntry.getRestrictionNames()) {
                final String restrictionDefined =
                        restrictionName + "=" + jrEntry.getRestriction(restrictionName).getString();
                boolean restrictionFound = false;
                for (final String restrictionFromClient : entrySendFromClient.restrictions) {
                    if (restrictionFromClient.equals(restrictionDefined)) {
                        restrictionFound = true;
                        break;
                    }
                }
                if (!restrictionFound) {
                    return false;
                }
            }
            return true;
        }
    }

}
