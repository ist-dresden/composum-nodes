package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.google.gson.GsonBuilder;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
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

    public enum Operation {users, user, groups, group}

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
