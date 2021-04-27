package com.composum.sling.core.usermanagement.core;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.usermanagement.model.AuthorizableModel;
import com.composum.sling.core.usermanagement.model.AuthorizablesTree;
import com.composum.sling.core.usermanagement.model.AuthorizablesView;
import com.composum.sling.core.usermanagement.model.GroupModel;
import com.composum.sling.core.usermanagement.model.TreeNode;
import com.composum.sling.core.usermanagement.model.UserModel;
import com.composum.sling.core.usermanagement.service.Authorizables;
import com.composum.sling.core.util.ResponseUtil;
import com.composum.sling.core.util.XSS;
import com.composum.sling.nodes.NodesConfiguration;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Mirko Zeibig
 * @since 26.10.2015
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes User Management Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + UserManagementServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_PUT,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_DELETE,
                "sling.auth.requirements=" + UserManagementServlet.SERVLET_PATH
        }
)
public class UserManagementServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UserManagementServlet.class);

    public static final String SERVLET_PATH = "/bin/cpm/usermanagement";

    public enum Extension {json, html}

    public enum Operation {users, user, groups, tree, group, authorizable, disable, enable, password, groupsofauthorizable, removefromgroup, addtogroup, query, systemuser, authorizables, properties}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Reference
    private NodesConfiguration coreConfig;

    @Reference
    protected Authorizables authorizablesService;

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
    protected ServletOperationSet<Extension, Operation> getOperations() {
        return operations;
    }

    public class GetTree implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            String path = AbstractServiceServlet.getPath(request);
            if (StringUtils.isBlank(path) || "/".equals(path)) {
                path = "/home";
            }
            final AuthorizablesTree tree = new AuthorizablesTree(context, null, null,
                    "^" + path + "(/.*)?");
            TreeNode node = tree.getRootNode().getNode(path);
            if (node != null) {
                try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                    node.toJson(jsonWriter, false);
                }
            }
        }
    }

    public abstract class GetAuthorizables<A extends Authorizable, E extends AuthorizableModel> implements ServletOperation {

        protected final Class<A> authorizableClass;

        public GetAuthorizables(Class<A> authorizableClass) {
            this.authorizableClass = authorizableClass;
        }

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response,
                         ResourceHandle resource)
                throws RepositoryException, IOException {

            AuthorizablesView view = createView(new Authorizables.Context(authorizablesService, request, response));

            response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
            response.setCharacterEncoding(MappingRules.CHARSET.name());
            try (JsonWriter writer = new JsonWriter(response.getWriter())) {
                writer.beginArray();
                for (AuthorizableModel model : view.getAuthorizableModels()) {
                    model.toJson(writer);
                }
                writer.endArray();
                writer.flush();
            }
        }

        protected AuthorizablesView createView(Authorizables.Context context)
                throws RepositoryException {
            return new AuthorizablesView(context, authorizableClass, null, null);
        }
    }

    public class GetAllAuthorizables extends GetAuthorizables<Authorizable, AuthorizableModel> {
        public GetAllAuthorizables() {
            super(Authorizable.class);
        }
    }

    public class GetUsers extends GetAuthorizables<User, UserModel> {
        public GetUsers() {
            super(User.class);
        }
    }

    public class GetGroups extends GetAuthorizables<Group, GroupModel> {
        public GetGroups() {
            super(Group.class);
        }
    }

    public class QueryAuthorizables extends GetAuthorizables<Authorizable, AuthorizableModel> {

        public QueryAuthorizables() {
            super(null);
        }

        @Override
        protected AuthorizablesView createView(Authorizables.Context context)
                throws RepositoryException {
            SlingHttpServletRequest request = context.getRequest();
            return new AuthorizablesView(context,
                    XSS.filter(request.getParameter("type")),
                    XSS.filter(request.getParameter("name")),
                    XSS.filter(request.getParameter("path")));
        }
    }

    public class GetGroupsOfAuthorizable implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final String path = AbstractServiceServlet.getPath(request);
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
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public class AddToGroup implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                String authorizableName = XSS.filter(request.getParameter("authorizable"));
                String groupName = XSS.filter(request.getParameter("group"));
                final Authorizable authorizable = userManager.getAuthorizable(authorizableName);
                final Group group = (Group) userManager.getAuthorizable(groupName);
                boolean b = group.addMember(authorizable);
                context.commit();
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public class RemoveFromGroup implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final Gson gson = new Gson();
                @SuppressWarnings("unchecked") final Map<String, String> p = gson.fromJson(
                        new InputStreamReader(request.getInputStream(), MappingRules.CHARSET.name()),
                        Map.class);
                String authorizableName = XSS.filter(p.get("authorizable"));
                String groupName = XSS.filter(p.get("group"));
                final Authorizable authorizable = userManager.getAuthorizable(authorizableName);
                final Group group = (Group) userManager.getAuthorizable(groupName);
                group.removeMember(authorizable);
                context.commit();
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public class GetProperties implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final String path = AbstractServiceServlet.getPath(request);
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
                } catch (RepositoryException e) {
                    // sling8 throws RepositoryException: Relative path foo refers to items outside of scope of authorizable.
                    ResponseUtil.writeEmptyArray(response);
                }
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public class ChangePassword implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                String username = XSS.filter(request.getParameter("username"));
                String password = XSS.filter(request.getParameter("password"));
                final Authorizable authorizable = userManager.getAuthorizable(username);
                final User target = (User) authorizable;
                final Authorizable agent = userManager.getAuthorizable(context.getResolver().getUserID());
                if (agent instanceof User && ((User) agent).isAdmin()) {
                    //admin can do it without knowing the old password for every user
                    target.changePassword(password);
                } else {
                    String oldPassword = XSS.filter(request.getParameter("oldPassword"));
                    target.changePassword(password, oldPassword);
                }
                context.commit();
                ResponseUtil.writeEmptyArray(response);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

    }

    public class DisableUser implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                String username = XSS.filter(request.getParameter("username"));
                String reason = XSS.filter(request.getParameter("reason"));
                final Authorizable authorizable = userManager.getAuthorizable(username);
                User user = (User) authorizable;
                user.disable(reason);
                context.commit();
                ResponseUtil.writeEmptyArray(response);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

    }

    public class EnableUser implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final String path = AbstractServiceServlet.getPath(request);
                final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
                User user = (User) authorizable;
                user.disable(null);
                context.commit();
                ResponseUtil.writeEmptyArray(response);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public class GetUser implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final String path = AbstractServiceServlet.getPath(request);
                final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
                if (authorizable == null) {
                    ResponseUtil.writeEmptyArray(response);
                } else {
                    User user = (User) authorizable;
                    UserModel userModel = new UserModel(context, user);
                    response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                    response.setCharacterEncoding(MappingRules.CHARSET.name());
                    try (JsonWriter writer = new JsonWriter(response.getWriter())) {
                        userModel.toJson(writer);
                        writer.flush();
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public class CreateSystemUser implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException, ServletException {
            try {
                final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
                final UserManager userManager = context.getUserManager();
                if (userManager != null) {
                    final String username = XSS.filter(request.getParameter("username"));
                    String intermediatePath = XSS.filter(request.getParameter("intermediatePath"));
                    if (StringUtils.isBlank(intermediatePath)) {
                        intermediatePath = null;
                    }
                    //public User createSystemUser(String userID, String intermediatePath)
                    Method method = userManager.getClass().getMethod("createSystemUser", String.class, String.class);
                    Object newUser = method.invoke(userManager, username, intermediatePath);
                    //
                    context.commit();
                    UserModel userModel = new UserModel(context, (User) newUser);
                    response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                    response.setCharacterEncoding(MappingRules.CHARSET.name());
                    try (JsonWriter writer = new JsonWriter(response.getWriter())) {
                        userModel.toJson(writer);
                        writer.flush();
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
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

    public class CreateUser implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            try {
                final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
                final UserManager userManager = context.getUserManager();
                if (userManager != null) {
                    final String username = XSS.filter(request.getParameter("username"));
                    String password = XSS.filter(request.getParameter("password"));
                    String intermediatePath = XSS.filter(request.getParameter("intermediatePath"));
                    final User newUser;
                    if (StringUtils.isEmpty(intermediatePath)) {
                        newUser = userManager.createUser(username, password);
                    } else {
                        newUser = userManager.createUser(username, password, () -> username, intermediatePath);
                    }
                    context.commit();
                    UserModel userModel = new UserModel(context, newUser);
                    response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                    response.setCharacterEncoding(MappingRules.CHARSET.name());
                    try (JsonWriter writer = new JsonWriter(response.getWriter())) {
                        userModel.toJson(writer);
                        writer.flush();
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        }
    }

    public class DeleteAuthorizable implements ServletOperation {

        @Override
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final String path = AbstractServiceServlet.getPath(request);
                String authorizableName = path.substring(path.lastIndexOf('/') + 1);
                if (authorizableName.equals("admin") || authorizableName.equals("anonymous")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, authorizableName + " deletion denied. System would have been destroyed.");
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
                        context.commit();
                        ResponseUtil.writeEmptyArray(response);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, authorizableName + " not found.");
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public class CreateGroup implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final String name = XSS.filter(request.getParameter("groupname"));
                String intermediatePath = XSS.filter(request.getParameter("intermediatePath"));
                final Group newGroup;
                if (StringUtils.isEmpty(intermediatePath)) {
                    newGroup = userManager.createGroup(name);
                } else {
                    newGroup = userManager.createGroup(name, () -> name, intermediatePath);
                }
                context.commit();
                GroupModel groupModel = new GroupModel(context, newGroup);
                response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                response.setCharacterEncoding(MappingRules.CHARSET.name());
                try (JsonWriter writer = new JsonWriter(response.getWriter())) {
                    groupModel.toJson(writer);
                    writer.flush();
                }
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public class GetGroup implements ServletOperation {

        @Override
        @SuppressWarnings("Duplicates")
        public void doIt(@NotNull final SlingHttpServletRequest request,
                         @NotNull final SlingHttpServletResponse response, ResourceHandle resource)
                throws RepositoryException, IOException {
            final Authorizables.Context context = new Authorizables.Context(authorizablesService, request, response);
            final UserManager userManager = context.getUserManager();
            if (userManager != null) {
                final String path = AbstractServiceServlet.getPath(request);
                final Authorizable authorizable = userManager.getAuthorizable(path.startsWith("/") ? path.substring(1) : path);
                if (authorizable == null) {
                    ResponseUtil.writeEmptyArray(response);
                } else {
                    Group group = (Group) authorizable;
                    GroupModel groupModel = new GroupModel(context, group);
                    response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
                    response.setCharacterEncoding(MappingRules.CHARSET.name());
                    try (JsonWriter writer = new JsonWriter(response.getWriter())) {
                        groupModel.toJson(writer);
                        writer.flush();
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
