package com.composum.sling.core.usermanagement.model;

import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AuthorizablesGraph {

    public interface UrlBuilder {

        @Nullable
        String buildUrl(AuthorizableModel node);
    }

    protected class Relation {

        protected final AuthorizableModel source;
        protected final AuthorizableModel target;

        public Relation(@NotNull final AuthorizableModel source, @NotNull final AuthorizableModel target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object other) {
            Relation otherRel;
            return other instanceof Relation &&
                    (otherRel = (Relation) other).target.equals(target) && otherRel.source.equals(source);
        }

        @Override
        public int hashCode() {
            return source.hashCode() + target.hashCode();
        }
    }

    public static final Map<String, Object> DEFAULT_NODE_CFG = new HashMap<String, Object>() {{
        put("style", "filled");
        put("fontname", "sans-serif");
        put("fontsize", "10.0");
    }};

    protected int index = 0;
    protected final AuthorizableModel singleFocus;
    protected final Map<String, Integer> indexes = new LinkedHashMap<>();
    protected final Map<String, AuthorizableModel> nodes = new TreeMap<>();
    protected final Set<Relation> targetRelations = new HashSet<>();
    protected final Set<Relation> sourceRelations = new HashSet<>();

    public AuthorizablesGraph(@NotNull final ResourceResolver resolver,
                              @Nullable final String selector,
                              @Nullable final String nameQueryPattern,
                              @Nullable final String pathPattern)
            throws RepositoryException {
        this(resolver, Authorizables.selector(selector), nameQueryPattern,
                StringUtils.isNotBlank(pathPattern) ? new Authorizables.Filter.Path(pathPattern) : null);
    }

    public AuthorizablesGraph(@NotNull final ResourceResolver resolver,
                              @Nullable final Class<? extends Authorizable> selector,
                              @Nullable final String nameQueryPattern,
                              @Nullable final Authorizables.Filter filter)
            throws RepositoryException {
        UserManager userManager = Authorizables.getUserManager(resolver);
        if (userManager != null) {
            Set<Authorizable> authorizables = Authorizables.findAuthorizables(
                    userManager, selector, nameQueryPattern, filter);
            for (Authorizable authorizable : authorizables) {
                addNode(authorizable);
            }
            singleFocus = nodes.size() == 1 ? nodes.values().iterator().next() : null;
            Set<String> singleFocusDone = singleFocus != null ? new HashSet<>() : null;
            for (AuthorizableModel source : nodes.values()) {
                addTargetRelations(userManager, source, singleFocusDone);
            }
            if (singleFocus != null) {
                addSourceRelations(userManager, selector, filter, singleFocus, singleFocusDone);
            }
        } else {
            singleFocus = null;
        }
    }

    protected AuthorizableModel addNode(@Nullable final Authorizable authorizable)
            throws RepositoryException {
        AuthorizableModel result = null;
        if (authorizable != null) {
            result = nodes.get(authorizable.getID());
            if (result == null) {
                result = authorizable.isGroup()
                        ? new GroupModel((Group) authorizable)
                        : new UserModel((User) authorizable);
                nodes.put(result.getId(), result);
                indexes.put(result.getId(), ++index);
            }
        }
        return result;
    }

    protected void addTargetRelations(@NotNull final UserManager userManager,
                                      @NotNull final AuthorizableModel source, @Nullable final Set<String> done)
            throws RepositoryException {
        String id = source.getId();
        if (done != null) {
            if (!done.add(id)) {
                return;
            }
        }
        for (String targetId : source.getDeclaredMemberOf()) {
            AuthorizableModel target = nodes.get(targetId);
            if (target == null && done != null) {
                target = addNode(userManager.getAuthorizable(targetId));
            }
            if (target != null) {
                targetRelations.add(new Relation(source, target));
                if (done != null) {
                    addTargetRelations(userManager, target, done);
                }
            }
        }
    }

    protected void addSourceRelations(@NotNull final UserManager userManager,
                                      @Nullable final Class<? extends Authorizable> selector,
                                      @Nullable final Authorizables.Filter filter,
                                      @NotNull final AuthorizableModel target, @NotNull final Set<String> done)
            throws RepositoryException {
        if (target.isGroup()) {
            Set<Authorizable> sources = Authorizables.findAuthorizables(
                    userManager, selector, null, authorizable
                            -> (filter == null || filter.accept(authorizable))
                            && isSourceOfTarget(authorizable, target.getId()));
            for (Authorizable source : sources) {
                if (done.add(source.getID())) {
                    sourceRelations.add(new Relation(addNode(source), target));
                }
            }
        }
    }

    protected boolean isSourceOfTarget(Authorizable source, String targetId)
            throws RepositoryException {
        Iterator<Group> targets = source.declaredMemberOf();
        while (targets.hasNext()) {
            if (targetId.equals(targets.next().getID())) {
                return true;
            }
        }
        return false;
    }

    public void toJson(@NotNull final JsonWriter writer) throws IOException {
        writer.beginObject();
        if (singleFocus != null) {
            writer.name("focus");
            singleFocus.toJson(writer);
            writer.name("sources").beginArray();
            for (Relation relation : sourceRelations) {
                relation.source.toJson(writer);
            }
            writer.endArray();
            writer.name("targets").beginArray();
            for (Relation relation : targetRelations) {
                relation.target.toJson(writer);
            }
            writer.endArray();
        } else {
            writer.name("nodes").beginArray();
            for (AuthorizableModel node : nodes.values()) {
                node.toJson(writer);
            }
            writer.endArray();
            writer.name("relations").beginArray();
            for (Relation relation : targetRelations) {
                writer.beginObject();
                writer.name("source").value(relation.source.getId());
                writer.name("target").value(relation.target.getId());
                writer.endObject();
            }
            writer.endArray();
        }
        writer.endObject();
    }

    public void toGraphviz(@NotNull final Writer writer,
                           @Nullable final Resource config, @Nullable final UrlBuilder urlBuilder)
            throws IOException {
        writer.append("digraph {\n");
        writeConfig(writer, config, "graph", null);
        writeConfig(writer, config, "node", DEFAULT_NODE_CFG);
        for (AuthorizableModel node : nodes.values()) {
            String id = node.getId();
            String path = node.getPath();
            String name = node.getPrincipalName();
            String label = !id.equals(name) ? id + "\n" + name : id;
            String url = urlBuilder != null ? urlBuilder.buildUrl(node) : null;
            writer.append("    ").append(indexes.get(node.getId()).toString()).append(" [");
            writer.append(" id=\"").append(id).append("\"");
            if (singleFocus != null && singleFocus.getId().equals(node.getId())) {
                writer.append(" root=\"true\"");
            }
            writer.append(" label=\"").append(label).append("\"");
            writer.append(" tooltip=\"").append(path).append("\"");
            if (StringUtils.isNotBlank(url)) {
                writer.append(" href=\"").append(url).append("\"");
            }
            writer.append(" fillcolor=\"").append(getColor(node)).append("\"");
            writer.append(" ]\n");
        }
        for (Relation relation : sourceRelations) {
            writer.append("    ").append(indexes.get(relation.source.getId()).toString())
                    .append(" -> ").append(indexes.get(relation.target.getId()).toString())
                    .append("\n");
        }
        for (Relation relation : targetRelations) {
            writer.append("    ").append(indexes.get(relation.source.getId()).toString())
                    .append(" -> ").append(indexes.get(relation.target.getId()).toString())
                    .append("\n");
        }
        writer.append("}\n");
    }

    protected void writeConfig(@NotNull final Writer writer, @Nullable final Resource configResource,
                               @NotNull final String type, @Nullable final Map<String, Object> defaultCfg)
            throws IOException {
        Resource cfgNode = configResource != null ? configResource.getChild(type) : null;
        Map<String, Object> config = cfgNode != null ? cfgNode.getValueMap() : defaultCfg;
        if (config != null) {
            writer.append("    ").append(type).append(" [");
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("jcr:")) {
                    Object value = entry.getValue();
                    if (value != null) {
                        writer.append(" ").append(key).append("=");
                        if (value instanceof String) {
                            writer.append("\"").append(value.toString()).append("\"");
                        } else {
                            writer.append(value.toString());
                        }
                    }
                }
            }
            writer.append(" ]\n");
        }
    }

    protected String getColor(AuthorizableModel node) {
        boolean isFocussed = singleFocus != null && singleFocus.getId().equals(node.getId());
        if (node.isGroup()) {
            return isFocussed ? "#4CBAC4" : "#C6FBFF";
        } else {
            UserModel user = (UserModel) node;
            if (user.isDisabled()) {
                return isFocussed ? "#E38585" : "#FFC2C2";
            } else if (user.isAdmin()) {
                return isFocussed ? "#54C478" : "#6EFD9D";
            } else if (user.isSystemUser()) {
                return isFocussed ? "#AEAEAE" : "#DDDDDD";
            } else {
                return isFocussed ? "#F1F55B" : "#FCFFBF";
            }
        }
    }
}
