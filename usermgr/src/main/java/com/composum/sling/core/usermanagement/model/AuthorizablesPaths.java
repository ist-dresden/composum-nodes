package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuthorizablesPaths extends AuthorizablesMap {

    public static final Map<String, Object> DEFAULT_NODE_CFG = new HashMap<String, Object>() {{
        put("style", "filled");
        put("fontname", "sans-serif");
        put("fontsize", "10.0");
    }};

    protected final String textPattern;

    public AuthorizablesPaths(@NotNull final Authorizables.Context context,
                              @Nullable final String selector,
                              @Nullable final String nameQueryPattern,
                              @Nullable final String pathPattern,
                              @Nullable final String textPattern)
            throws RepositoryException {
        this(context, Authorizables.selector(selector), nameQueryPattern,
                StringUtils.isNotBlank(pathPattern) ? new Authorizables.Filter.Path(pathPattern) : null,
                textPattern);
    }

    public AuthorizablesPaths(@NotNull final Authorizables.Context context,
                              @Nullable final Class<? extends Authorizable> selector,
                              @Nullable final String nameQueryPattern,
                              @Nullable final Authorizables.Filter filter,
                              @Nullable final String textPattern)
            throws RepositoryException {
        super(context, selector, nameQueryPattern, filter);
        this.textPattern = textPattern;
    }

    public void toPathsTable(@NotNull final ResourceResolver resolver,
                             @NotNull final Writer writer, @Nullable final Resource config,
                             @Nullable final AuthorizablesView.NodeUrlBuilder nodeUrlBuilder,
                             @Nullable final AuthorizablesView.PathUrlBuilder pathUrlBuilder)
            throws IOException {
        writer.append("<table class=\"composum-nodes-usermgr-graph_table\">\n  <tbody>\n");
        for (AuthorizableModel node : nodes.values()) {
            AuthorizableAcls acls = new AuthorizableAcls(resolver, node.getId());
            Map<String, AuthorizableAcls.AcRuleSet> affectedPaths = acls.getAffectedPaths();
            Iterator<String> pathSet = affectedPaths.keySet().iterator();
            writer.append("    <tr class=\"").append(getNodeClass(node)).append("\">\n");
            writer.append("      <td class=\"authorizable-id\" rowspan=\"")
                    .append(Integer.toString(Math.max(1, affectedPaths.size())))
                    .append("\">");
            String url = null;
            if (nodeUrlBuilder != null && StringUtils.isNotBlank(url = nodeUrlBuilder.buildUrl(node))) {
                writer.append("<a href=\"").append(url).append("\">");
            }
            writer.append(node.getId());
            if (url != null) {
                writer.append("</a>");
            }
            writer.append("</td>\n      ");
            writePath(writer, node, pathUrlBuilder, affectedPaths, pathSet.hasNext() ? pathSet.next() : null, true);
            while (pathSet.hasNext()) {
                writePath(writer, node, pathUrlBuilder, affectedPaths, pathSet.next(), false);
            }
            writer.append("    </tr>\n");
        }
        writer.append("  </tbody>\n</table>\n");
    }

    protected void writePath(@NotNull final Writer writer, @NotNull final AuthorizableModel node,
                             @Nullable final AuthorizablesView.PathUrlBuilder pathUrlBuilder,
                             @NotNull final Map<String, AuthorizableAcls.AcRuleSet> affectedPaths,
                             @Nullable final String path, boolean firstLine)
            throws IOException {
        if (!firstLine) {
            writer.append("    </tr>\n");
            writer.append("    <tr class=\"").append(getNodeClass(node)).append("\">\n      ");
        }
        writer.append("<td class=\"affected-path\"");
        if (path == null) {
            writer.append("colspan=\"2\"");
        }
        writer.append(">");
        String url = null;
        if (path != null && pathUrlBuilder != null
                && StringUtils.isNotBlank(url = pathUrlBuilder.buildUrl(node, path))) {
            writer.append("<a href=\"").append(url).append("\" data-path=\"").append(path).append("\">");
        }
        writer.append(path != null ? path : "no affected paths found");
        if (url != null) {
            writer.append("</a>");
        }
        writer.append("</td>");
        if (path != null) {
            writeRules(writer, affectedPaths.get(path));
        }
        writer.append("\n");
    }

    protected void writeRules(@NotNull final Writer writer,
                              @NotNull final AuthorizableAcls.AcRuleSet ruleSet)
            throws IOException {
        writer.append("<td class=\"ac-rules\">");
        int count = 0;
        for (AuthorizableAcls.AcRule rule : ruleSet.getRules()) {
            if (count > 0) {
                writer.append(" / ");
            }
            count++;
            writer.append("<span class=\"").append(rule.getType().name().toLowerCase()).append("\">")
                    .append(rule.toString()).append("</span>");
        }
        writer.append("</td>");
    }
}
