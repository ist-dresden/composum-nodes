package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AuthorizablesGraph extends AuthorizablesView {

    public static final Map<String, Object> DEFAULT_NODE_CFG = new HashMap<String, Object>() {{
        put("style", "filled");
        put("fontname", "sans-serif");
        put("fontsize", "10.0");
    }};

    public AuthorizablesGraph(@NotNull final Authorizables.Context context,
                              @Nullable final String selector,
                              @Nullable final String nameQueryPattern,
                              @Nullable final String pathPattern)
            throws RepositoryException {
        this(context, Authorizables.selector(selector), nameQueryPattern,
                StringUtils.isNotBlank(pathPattern) ? new Authorizables.Filter.Path(pathPattern) : null);
    }

    public AuthorizablesGraph(@NotNull final Authorizables.Context context,
                              @Nullable final Class<? extends Authorizable> selector,
                              @Nullable final String nameQueryPattern,
                              @Nullable final Authorizables.Filter filter)
            throws RepositoryException {
        super(context, selector, nameQueryPattern, filter);
    }

    @Override
    protected void extendedScan(@Nullable final Class<? extends Authorizable> selector,
                                @Nullable final Authorizables.Filter filter,
                                @Nullable final Set<String> singleFocusDone)
            throws RepositoryException {
        if (singleFocus != null && singleFocusDone != null) {
            addSourceRelations(selector, filter, singleFocus, singleFocusDone);
        }
    }

    public void toGraphviz(@NotNull final Writer writer,
                           @Nullable final Resource config, @Nullable final AuthorizablesView.NodeUrlBuilder urlBuilder)
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
            return isFocussed ? "#4cbac4" : "#c6fbff";
        } else {
            UserModel user = (UserModel) node;
            if (user.isDisabled()) {
                return isFocussed ? "#e38585" : "#ffc2c2";
            } else if (user.isAdmin()) {
                return isFocussed ? "#54c478" : "#6efd9d";
            } else if (user.isServiceUser()) {
                return isFocussed ? "#d6aa69" : "#e5d2ba";
            } else if (user.isSystemUser()) {
                return isFocussed ? "#aeaeae" : "#dddddd";
            } else {
                return isFocussed ? "#fbff52" : "#fbffb3";
            }
        }
    }
}
