package com.composum.sling.core.usermanagement.core;

import com.composum.sling.nodes.NodesConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mirko Zeibig
 * @since 26.10.2015
 */
@SlingServlet(
        paths = "/bin/cpm/usermanagement",
        methods = {"GET", "PUT", "POST", "DELETE"}
)
public class UserManagementServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UserManagementServlet.class);

    public enum Extension {json, html}

    public enum Operation {users, user, groups, tree, group, authorizable, disable, enable, password, groupsofauthorizable, removefromgroup, addtogroup, query, systemuser, authorizables, properties}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Reference
    private NodesConfiguration coreConfig;

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.authorizables, new GetAllAuthorizables());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.users, new GetUsers());
        // curl -u admin:admin http://localhost:9090/bin/cpm/usermanagement.user.json/eeee
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.user, new GetUser());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.groups, new GetGroups());
        // curl -u admin:admin http://localhost:9090/bin/cpm/usermanagement.group.json/mygroup
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.group, new GetGroup());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.tree, new GetTree());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.properties, new GetProperties());
        // curl -u admin:admin http://localhost:9090/bin/cpm/usermanagement.groupsofauthorizable.json/eeee
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.groupsofauthorizable, new GetGroupsOfAuthorizable());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.query, new QueryAuthorizables());

        // POST
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.user, new CreateUser());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.systemuser, new CreateSystemUser());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.group, new CreateGroup());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.disable, new DisableUser());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.enable, new EnableUser());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.password, new ChangePassword());

        operations.setOperation(ServletOperationSet.Method.PUT, Extension.json, Operation.removefromgroup, new RemoveFromGroup());
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.addtogroup, new AddToGroup());

        // DELETE
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json, Operation.authorizable, new DeleteAuthorizable());
    }

    @Override
    protected ServletOperationSet getOperations() {
        return operations;
    }

    static class AuthorizableEntry {
        String id;
        String path;
        String[] memberOf = {};
        String[] declaredMemberOf = {};
        String[] members = {};
        String[] declaredMembers = {};
        String principalName;
        boolean isGroup;
        boolean systemUser;

        protected static String[] getIDs(Iterator<? extends Authorizable> authorizableIterator) throws RepositoryException {
            List<String> strings = new ArrayList<>();
            while (authorizableIterator.hasNext()) {
                Authorizable authorizable = authorizableIterator.next();
                strings.add(authorizable.getID());
            }
            return strings.toArray(new String[0]);
        }
    }

    static class UserEntry extends AuthorizableEntry {
        boolean admin;
        boolean disabled;
        String disabledReason;
        Map<String, Object> properties = new HashMap<>();

        static UserEntry fromUser(User user) throws RepositoryException {
            UserEntry userEntry = new UserEntry();
            Iterator<Group> groupIterator = user.memberOf();
            Iterator<Group> declaredMemberOf = user.declaredMemberOf();
            Iterator<String> propertyNames = user.getPropertyNames();
            userEntry.id = user.getID();
            userEntry.path = user.getPath();
            userEntry.admin = user.isAdmin();
            userEntry.disabled = user.isDisabled();
            userEntry.disabledReason = user.getDisabledReason();
            userEntry.principalName = user.getPrincipal().getName();
            userEntry.memberOf = getIDs(groupIterator);
            userEntry.declaredMemberOf = getIDs(declaredMemberOf);
            userEntry.isGroup = false;
            userEntry.systemUser = isSystemUser(user);
            while (propertyNames.hasNext()) {
                String name = propertyNames.next();
                Value[] property = user.getProperty(name);
                String[] vs = new String[property.length];
                for (int i = 0; i < property.length; i++) {
                    vs[i] = property[i].getString();
                }
                userEntry.properties.put(name, vs);
            }
            return userEntry;
        }


    }

    protected static boolean isSystemUser(Authorizable user) {
        boolean su;//public boolean isSystemUser()
        try {
            Method method = user.getClass().getMethod("isSystemUser");
            method.setAccessible(true);
            Boolean result = (Boolean) method.invoke(user);
            su = (result != null && result);
        } catch (Exception e) {
            //ignore
            su = false;
        }
        return su;
    }

    static class GroupEntry extends AuthorizableEntry {
        static GroupEntry fromGroup(Group group) throws RepositoryException {
            GroupEntry groupEntry = new GroupEntry();
            Iterator<Group> groupIterator = group.memberOf();
            Iterator<Group> declaredMemberOf = group.declaredMemberOf();
            Iterator<Authorizable> members = group.getMembers();
            Iterator<Authorizable> declaredMembers = group.getDeclaredMembers();
            groupEntry.id = group.getID();
            groupEntry.path = group.getPath();
            groupEntry.principalName = group.getPrincipal().getName();
            groupEntry.memberOf = getIDs(groupIterator);
            groupEntry.declaredMemberOf = getIDs(declaredMemberOf);
            groupEntry.members = getIDs(members);
            groupEntry.declaredMembers = getIDs(declaredMembers);
            groupEntry.isGroup = true;
            return groupEntry;
        }
    }

    public static abstract class GetAuthorizables<A extends Authorizable, E extends AuthorizableEntry> implements ServletOperation {

        private final Class<A> authorizableClass;

        public GetAuthorizables(Class<A> authorizableClass) {
            this.authorizableClass = authorizableClass;
        }

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            final Query q = new Query() {
                @Override
                public <T> void build(final QueryBuilder<T> builder) {
                    builder.setCondition(builder.nameMatches("%"));
                    builder.setSortOrder("@name", QueryBuilder.Direction.ASCENDING);
                    builder.setSelector(authorizableClass);
                }
            };
            final Iterator<Authorizable> principals = userManager.findAuthorizables(q);

            List<E> entries = new ArrayList<>();

            while (principals.hasNext()) {
                E entry = processPrincipal(authorizableClass.cast(principals.next()));
                entries.add(entry);
            }

            String s = new GsonBuilder().create().toJson(entries);
            response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
            response.setCharacterEncoding(MappingRules.CHARSET.name());
            PrintWriter writer = response.getWriter();
            writer.write(s);
            writer.flush();
            writer.close();

        }

        protected abstract E processPrincipal(A authorizable) throws RepositoryException;

        protected String[] getIDs(Iterator<Group> groupIterator) throws RepositoryException {
            List<String> strings = new ArrayList<>();
            while (groupIterator.hasNext()) {
                Group group = groupIterator.next();
                strings.add(group.getID());
            }
            return strings.toArray(new String[0]);
        }
    }

    public static class QueryAuthorizables implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            final String path = AbstractServiceServlet.getPath(request);
            final Query q = new Query() {
                @Override
                public <T> void build(final QueryBuilder<T> builder) {
                    builder.setCondition(builder.nameMatches("%" + (path.startsWith("/") ? path.substring(1) : path) + "%"));
                    builder.setSortOrder("@name", QueryBuilder.Direction.ASCENDING);
                    builder.setSelector(Authorizable.class);
                }
            };
            final Iterator<Authorizable> principals = userManager.findAuthorizables(q);

            List<AuthorizableEntry> entries = new ArrayList<>();

            while (principals.hasNext()) {
                AuthorizableEntry entry = processPrincipal(principals.next());
                entries.add(entry);
            }

            String s = new GsonBuilder().create().toJson(entries);
            PrintWriter writer = response.getWriter();
            writer.write(s);
            writer.flush();
            writer.close();

        }

        protected AuthorizableEntry processPrincipal(Authorizable authorizable) throws RepositoryException {
            AuthorizableEntry authorizableEntry = new AuthorizableEntry();
            Principal principal = authorizable.getPrincipal();
            authorizableEntry.id = authorizable.getID();
            authorizableEntry.path = authorizable.getPath();
            authorizableEntry.principalName = principal.getName();
            authorizableEntry.isGroup = authorizable.isGroup();
            authorizableEntry.systemUser = isSystemUser(authorizable);
            return authorizableEntry;
        }
    }

    public static class GetGroupsOfAuthorizable implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final String path = AbstractServiceServlet.getPath(request);
            final UserManager userManager = session.getUserManager();
            final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
            Iterator<Group> groupIterator = authorizable.declaredMemberOf();
            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                jsonWriter.beginArray();
                while (groupIterator.hasNext()) {
                    Group group = groupIterator.next();
                    jsonWriter
                            .value(group.getID());
                }
                jsonWriter.endArray();
                jsonWriter.flush();
            }

        }
    }

    public static class AddToGroup implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            String authorizableName = request.getParameter("authorizable");
            String groupName = request.getParameter("group");
            final Authorizable authorizable = userManager.getAuthorizable(authorizableName);
            final Group group = (Group) userManager.getAuthorizable(groupName);
            boolean b = group.addMember(authorizable);
            session.save();
        }
    }

    public static class RemoveFromGroup implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();

            final Gson gson = new Gson();
            @SuppressWarnings("unchecked") final Map<String, String> p = gson.fromJson(
                    new InputStreamReader(request.getInputStream(), MappingRules.CHARSET.name()),
                    Map.class);
            String authorizableName = p.get("authorizable");
            String groupName = p.get("group");
            final Authorizable authorizable = userManager.getAuthorizable(authorizableName);
            final Group group = (Group) userManager.getAuthorizable(groupName);
            group.removeMember(authorizable);
            session.save();
        }
    }

    public static class GetTree implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            String originalRequestPath = AbstractServiceServlet.getPath(request);
            String requestPath;
            if (!originalRequestPath.endsWith("/")) {
                requestPath = originalRequestPath + "/";
            } else {
                requestPath = originalRequestPath;
            }
            int requestPathLength = requestPath.length();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            final Query q = new Query() {
                @Override
                public <T> void build(final QueryBuilder<T> builder) {
                    builder.setCondition(builder.nameMatches("%"));
                    builder.setSortOrder("@name", QueryBuilder.Direction.ASCENDING);
                    builder.setSelector(Authorizable.class);
                }
            };
            final Iterator<Authorizable> authorizables = userManager.findAuthorizables(q);
            Set<String> paths = new HashSet<>();
            Set<Authorizable> auths = new HashSet<>();
            while (authorizables.hasNext()) {
                Authorizable authorizable = authorizables.next();
                String path = authorizable.getPath();
                if (path.startsWith(requestPath)) {
                    int firstSlashPositionAfterRequestPath = path.substring(requestPathLength).indexOf('/');
                    if (firstSlashPositionAfterRequestPath > 0) {
                        //child nodes are relative paths
                        String segment = path.substring(requestPathLength, requestPathLength + firstSlashPositionAfterRequestPath);
                        paths.add(segment);
                    } else {
                        //child nodes are authorizables
                        auths.add(authorizable);
                    }
                }
            }

            Authorizable authorizableByRequestPath = userManager.getAuthorizableByPath(requestPath);

            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                if (authorizableByRequestPath == null) {
                    String name = originalRequestPath.equals("/")
                            ? "/" : originalRequestPath.substring(originalRequestPath.lastIndexOf('/') + 1);
                    jsonWriter.beginObject()
                            .name("id").value(originalRequestPath)
                            .name("text").value(name)
                            .name("name").value(name)
                            .name("path").value(originalRequestPath)
                            .name("children")
                            .beginArray();
                } else {
                    //noinspection Duplicates
                    jsonWriter.beginObject()
                            .name("id").value(authorizableByRequestPath.getPath())
                            .name("text").value(authorizableByRequestPath.getID())
                            .name("name").value(authorizableByRequestPath.getID())
                            .name("path").value(authorizableByRequestPath.getPath())
                            .name("type").value(authorizableByRequestPath.isGroup() ? "group" : "user")
                            .name("disabled").value(!authorizableByRequestPath.isGroup() && ((User) authorizableByRequestPath).isDisabled())
                            .name("systemUser").value(isSystemUser(authorizableByRequestPath))
                            .name("state").beginObject().name("loaded").value(true).endObject()
                            .name("children")
                            .beginArray();
                }
                for (String path : paths) {
                    jsonWriter.beginObject()
                            .name("id").value(requestPath + path)
                            .name("text").value(path)
                            .name("name").value(path)
                            .name("path").value(requestPath + path)
                            .name("state").beginObject().name("loaded").value(false).endObject()
                            .endObject();
                }
                for (Authorizable authorizable : auths) {
                    //noinspection Duplicates
                    jsonWriter.beginObject()
                            .name("id").value(authorizable.getPath())
                            .name("text").value(authorizable.getID())
                            .name("name").value(authorizable.getID())
                            .name("path").value(authorizable.getPath())
                            .name("type").value(authorizable.isGroup() ? "group" : "user")
                            .name("disabled").value(!authorizable.isGroup() && ((User) authorizable).isDisabled())
                            .name("systemUser").value(isSystemUser(authorizable))
                            .name("state").beginObject().name("loaded").value(true).endObject()
                            .endObject();
                }

                jsonWriter.endArray().endObject();
                jsonWriter.flush();
            }
        }
    }

    public static class GetProperties implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final String path = AbstractServiceServlet.getPath(request);
            final UserManager userManager = session.getUserManager();
            String[] split = path.split("/");
            String userid = split[1];
            String propPath = split[2];
            final Authorizable authorizable = userManager.getAuthorizable(userid);
            try {
                Iterator<String> propertyNames = authorizable.getPropertyNames(propPath);
                Map<String, String> p = new HashMap<>();
                while (propertyNames.hasNext()) {
                    String name = propertyNames.next();
                    Value[] property = authorizable.getProperty(propPath + "/" + name);
                    p.put(name, property[0].getString());
                }
                try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                    jsonWriter.beginArray();
                    for (final Map.Entry<String, String> e : p.entrySet()) {
                        jsonWriter
                                .beginObject()
                                .name("name")
                                .value(e.getKey())
                                .name("value")
                                .value(e.getValue())
                                .endObject();
                    }
                    jsonWriter.endArray();
                    jsonWriter.flush();
                }
            } catch (/*PathNotFoundException |*/ RepositoryException e) {
                // sling8 throws RepositoryException: Relative path foo refers to items outside of scope of authorizable.
                ResponseUtil.writeEmptyArray(response);
            }
        }
    }

    public static class ChangePassword implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            final Authorizable authorizable = userManager.getAuthorizable(username);
            final User target = (User) authorizable;
            final Authorizable agent = userManager.getAuthorizable(resolver.getUserID());

            if (agent instanceof User && ((User) agent).isAdmin()) {
                //admin can do it without knowing the old password for every user
                target.changePassword(password);
            } else {
                String oldPassword = request.getParameter("oldPassword");
                target.changePassword(password, oldPassword);
            }

            session.save();
            ResponseUtil.writeEmptyArray(response);
        }

    }

    public static class DisableUser implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            String username = request.getParameter("username");
            String reason = request.getParameter("reason");

            final Authorizable authorizable = userManager.getAuthorizable(username);
            User user = (User) authorizable;
            user.disable(reason);
            session.save();
            ResponseUtil.writeEmptyArray(response);
        }

    }

    public static class EnableUser implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final String path = AbstractServiceServlet.getPath(request);
            final UserManager userManager = session.getUserManager();
            final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
            User user = (User) authorizable;
            user.disable(null);
            session.save();
            ResponseUtil.writeEmptyArray(response);
        }
    }

    public static class GetUser implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final String path = AbstractServiceServlet.getPath(request);
            if (path == null) {
                ResponseUtil.writeEmptyArray(response);
            } else {
                final UserManager userManager = session.getUserManager();
                final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
                if (authorizable == null) {
                    ResponseUtil.writeEmptyArray(response);
                } else {
                    User user = (User) authorizable;
                    UserEntry userEntry = UserEntry.fromUser(user);
                    String s = new GsonBuilder().create().toJson(userEntry);
                    response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                    response.setCharacterEncoding(MappingRules.CHARSET.name());
                    PrintWriter writer = response.getWriter();
                    writer.write(s);
                    writer.write('\n');
                    writer.flush();
                    writer.close();
                }
            }
        }
    }

    public class CreateSystemUser implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final UserManager userManager = session.getUserManager();
                final String username = request.getParameter("username");
                String intermediatePath = request.getParameter("intermediatePath");
                //public User createSystemUser(String userID, String intermediatePath)
                Method method = userManager.getClass().getMethod("createSystemUser", String.class, String.class);
                Object newUser = method.invoke(userManager, username, StringUtils.isEmpty(intermediatePath) ? null : intermediatePath);
                session.save();
                UserEntry userEntry = UserEntry.fromUser((User) newUser);
                String s = new GsonBuilder().create().toJson(userEntry);
                PrintWriter writer = response.getWriter();
                response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                response.setCharacterEncoding(MappingRules.CHARSET.name());
                writer.write(s);
                writer.write('\n');
                writer.flush();
                writer.close();
            } catch (NoSuchMethodException | IllegalAccessException e) {
                // ignore. server too old
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "createSystemUser is not supported on your system");
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RepositoryException) {
                    throw (RepositoryException) cause;
                } else {
                    throw new ServletException(cause);
                }
            }
        }
    }

    public static class CreateUser implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
                final UserManager userManager = session.getUserManager();
                final String username = request.getParameter("username");
                String password = request.getParameter("password");
                String intermediatePath = request.getParameter("intermediatePath");
                final User newUser;
                if (StringUtils.isEmpty(intermediatePath)) {
                    newUser = userManager.createUser(username, password);
                } else {
                    newUser = userManager.createUser(username, password, new Principal() {
                        @Override
                        public String getName() {
                            return username;
                        }
                    }, intermediatePath);
                }
                session.save();
                UserEntry userEntry = UserEntry.fromUser(newUser);
                String s = new GsonBuilder().create().toJson(userEntry);
                response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                response.setCharacterEncoding(MappingRules.CHARSET.name());
                PrintWriter writer = response.getWriter();
                writer.write(s);
                writer.write('\n');
                writer.flush();
                writer.close();

            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        }
    }

    public static class DeleteAuthorizable implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            final String path = AbstractServiceServlet.getPath(request);
            if (path != null) {
                String authorizableName = path.substring(path.lastIndexOf('/') + 1);
                if (authorizableName.equals("admin") || authorizableName.equals("anonymous")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, authorizableName + " deleted. System destroyed.");
                } else {
                    Authorizable authorizable = userManager.getAuthorizable(authorizableName);
                    if (authorizable == null) {
                        authorizable = userManager.getAuthorizableByPath(path);
                    }
                    if (authorizable != null) {
                        Iterator<Group> groupIterator = authorizable.declaredMemberOf();
                        while (groupIterator.hasNext()) {
                            Group group = groupIterator.next();
                            group.removeMember(authorizable);
                        }
                        authorizable.remove();
                        session.save();
                        ResponseUtil.writeEmptyArray(response);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, authorizableName + " not found.");
                    }
                }
            } else {
                ResponseUtil.writeEmptyArray(response);
            }
        }
    }

    public static class CreateGroup implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            final String name = request.getParameter("groupname");
            String intermediatePath = request.getParameter("intermediatePath");
            final Group newGroup;
            if (StringUtils.isEmpty(intermediatePath)) {
                newGroup = userManager.createGroup(name);
            } else {
                newGroup = userManager.createGroup(name, new Principal() {
                    @Override
                    public String getName() {
                        return name;
                    }
                }, intermediatePath);
            }
            session.save();
            GroupEntry groupEntry = GroupEntry.fromGroup(newGroup);
            String s = new GsonBuilder().create().toJson(groupEntry);
            response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
            response.setCharacterEncoding(MappingRules.CHARSET.name());
            PrintWriter writer = response.getWriter();
            writer.write(s);
            writer.write('\n');
            writer.flush();
            writer.close();
        }
    }

    public static class GetGroup implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final String path = AbstractServiceServlet.getPath(request);
            if (path == null) {
                ResponseUtil.writeEmptyArray(response);
            } else {
                final UserManager userManager = session.getUserManager();
                final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
                if (authorizable == null) {
                    ResponseUtil.writeEmptyArray(response);
                } else {
                    Group group = (Group) authorizable;
                    GroupEntry groupEntry = GroupEntry.fromGroup(group);
                    String s = new GsonBuilder().create().toJson(groupEntry);
                    response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                    response.setCharacterEncoding(MappingRules.CHARSET.name());
                    PrintWriter writer = response.getWriter();
                    writer.write(s);
                    writer.write('\n');
                    writer.flush();
                    writer.close();
                }
            }
        }

    }

    public static class GetAllAuthorizables extends GetAuthorizables<Authorizable, AuthorizableEntry> {
        public GetAllAuthorizables() {
            super(Authorizable.class);
        }

        @Override
        @SuppressWarnings("Duplicates")
        protected AuthorizableEntry processPrincipal(Authorizable authorizable) throws RepositoryException {
            AuthorizableEntry authorizableEntry = new AuthorizableEntry();
            Principal principal = authorizable.getPrincipal();
            Iterator<Group> groupIterator = authorizable.memberOf();
            Iterator<Group> declaredMemberOf = authorizable.declaredMemberOf();
            authorizableEntry.id = authorizable.getID();
            authorizableEntry.path = authorizable.getPath();
            authorizableEntry.principalName = principal.getName();
            authorizableEntry.memberOf = getIDs(groupIterator);
            authorizableEntry.declaredMemberOf = getIDs(declaredMemberOf);
            authorizableEntry.isGroup = authorizable.isGroup();
            return authorizableEntry;
        }
    }

    public static class GetUsers extends GetAuthorizables<User, UserEntry> {
        public GetUsers() {
            super(User.class);
        }

        @Override
        protected UserEntry processPrincipal(User user) throws RepositoryException {
            return UserEntry.fromUser(user);
        }
    }

    public static class GetGroups extends GetAuthorizables<Group, GroupEntry> {

        public GetGroups() {
            super(Group.class);
        }

        @Override
        protected GroupEntry processPrincipal(Group group) throws RepositoryException {
            GroupEntry groupEntry = new GroupEntry();
            Principal principal = group.getPrincipal();
            Iterator<Group> groupIterator = group.memberOf();
            Iterator<Group> declaredMemberOf = group.declaredMemberOf();
            Iterator<String> propertyNames = group.getPropertyNames();
            groupEntry.id = group.getID();
            groupEntry.path = group.getPath();
            groupEntry.principalName = principal.getName();
            groupEntry.memberOf = getIDs(groupIterator);
            groupEntry.declaredMemberOf = getIDs(declaredMemberOf);
            groupEntry.isGroup = true;
            return groupEntry;
        }
    }

}
