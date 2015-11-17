package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.*;

/**
 * @author Mirko Zeibig
 * @since 26.10.2015
 */
@SlingServlet(
        paths = "/bin/core/usermanagement",
        methods = {"GET", "PUT", "POST"}
)
public class UserManagementServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UserManagementServlet.class);

    public enum Extension {json, html}

    public enum Operation {users, user, groups, tree, group}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Reference
    private CoreConfiguration coreConfig;

    @Override protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.users, new GetUsers());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.user, new GetUser());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.groups, new GetGroups());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.group, new GetGroup());
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.tree, new GetTree());

    }

    @Override protected ServletOperationSet getOperations() {
        return operations;
    }

    static class AuthorizableEntry {
        String id;
        String path;
        String[] memberOf = {};
        String[] declaredMemberOf = {};
        String principalName;
        protected static String[] getIDs(Iterator<Group> groupIterator) throws RepositoryException {
            List<String> strings = new ArrayList<>();
            while (groupIterator.hasNext()) {
                Group group = groupIterator.next();
                strings.add(group.getID());
            }
            return strings.toArray(new String[strings.size()]);
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

    static class GroupEntry extends AuthorizableEntry {
        static GroupEntry fromGroup(Group group) throws RepositoryException {
            GroupEntry groupEntry = new GroupEntry();
            Iterator<Group> groupIterator = group.memberOf();
            Iterator<Group> declaredMemberOf = group.declaredMemberOf();
            groupEntry.id = group.getID();
            groupEntry.path = group.getPath();
            groupEntry.principalName = group.getPrincipal().getName();
            groupEntry.memberOf = getIDs(groupIterator);
            groupEntry.declaredMemberOf = getIDs(declaredMemberOf);
            return groupEntry;
        }
    }

    public static abstract class GetAuthorizables<A extends Authorizable, E extends AuthorizableEntry> implements ServletOperation {

        private final Class<A> authorizableClass;

        public GetAuthorizables(Class<A> authorizableClass) {
            this.authorizableClass = authorizableClass;
        }

        @Override public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final UserManager userManager = session.getUserManager();
            final Query q = new Query() {
                @Override public <T> void build(final QueryBuilder<T> builder) {
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
            return strings.toArray(new String[strings.size()]);
        }
    }

    public static class GetTree implements ServletOperation {

        @Override public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
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
                @Override public <T> void build(final QueryBuilder<T> builder) {
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
                        if (!paths.contains(segment)) {
                            paths.add(segment);
                        }
                    } else {
                        //child nodes are authorizables
                        auths.add(authorizable);
                    }
                }
            }

            Authorizable authorizableByRequestPath = userManager.getAuthorizableByPath(requestPath);

            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                if (authorizableByRequestPath == null) {
                    jsonWriter.beginObject()
                            .name("id").value(originalRequestPath)
                            .name("text").value(originalRequestPath.substring(originalRequestPath.lastIndexOf('/') + 1))
                            .name("name").value(originalRequestPath.substring(originalRequestPath.lastIndexOf('/') + 1))
                            .name("path").value(originalRequestPath)
                            .name("children")
                            .beginArray();
                } else {
                    jsonWriter.beginObject()
                            .name("id").value(authorizableByRequestPath.getPath())
                            .name("text").value(authorizableByRequestPath.getID())
                            .name("name").value(authorizableByRequestPath.getID())
                            .name("path").value(authorizableByRequestPath.getPath())
                            .name("type").value(authorizableByRequestPath.isGroup()?"group":"user")
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
                    jsonWriter.beginObject()
                            .name("id").value(authorizable.getPath())
                            .name("text").value(authorizable.getID())
                            .name("name").value(authorizable.getID())
                            .name("path").value(authorizable.getPath())
                            .name("type").value(authorizable.isGroup()?"group":"user")
                            .name("state").beginObject().name("loaded").value(true).endObject()
                            .endObject();
                }

                jsonWriter.endArray().endObject();
                jsonWriter.flush();
            }
        }
    }

    public static class GetUser implements ServletOperation {

        @Override public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final String path = AbstractServiceServlet.getPath(request);
            final UserManager userManager = session.getUserManager();
            final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
            User user = (User) authorizable;
            UserEntry userEntry = UserEntry.fromUser(user);
            String s = new GsonBuilder().create().toJson(userEntry);
            PrintWriter writer = response.getWriter();
            writer.write(s);
            writer.write('\n');
            writer.flush();
            writer.close();

        }

    }

    public static class GetGroup implements ServletOperation {

        @Override public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            final String path = AbstractServiceServlet.getPath(request);
            final UserManager userManager = session.getUserManager();
            final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
            Group group = (Group) authorizable;
            GroupEntry groupEntry = GroupEntry.fromGroup(group);
            String s = new GsonBuilder().create().toJson(groupEntry);
            PrintWriter writer = response.getWriter();
            writer.write(s);
            writer.write('\n');
            writer.flush();
            writer.close();

        }

    }

    public static class GetUsers extends GetAuthorizables<User, UserEntry> {
        public GetUsers() {
            super(User.class);
        }

        @Override protected UserEntry processPrincipal(User user) throws RepositoryException {
            return UserEntry.fromUser(user);
        }
    }

    public static class GetGroups extends GetAuthorizables<Group, GroupEntry> {

        public GetGroups() {
            super(Group.class);
        }

        @Override protected GroupEntry processPrincipal(Group group) throws RepositoryException {
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
            return groupEntry;
        }
    }

}
