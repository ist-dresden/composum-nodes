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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AuthorizablesPaths extends AuthorizablesMap {

    public static final Map<String, Object> DEFAULT_NODE_CFG = new HashMap<String, Object>() {{
        put("style", "filled");
        put("fontname", "sans-serif");
        put("fontsize", "10.0");
    }};

    protected final Pattern textPattern;

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
        this.textPattern = StringUtils.isNotBlank(textPattern)
                ? Authorizables.Filter.createPattern(textPattern)
                : null;
    }

    @NotNull
    protected Collection<String> filterPaths(@NotNull final Collection<String> pathSet) {
        List<String> result = new ArrayList<>();
        for (String path : pathSet) {
            if (this.textPattern == null || textPattern.matcher(path).matches()) {
                result.add(path);
            }
        }
        return result;
    }

    public void toPathsTable(@NotNull final ResourceResolver resolver,
                             @NotNull final Writer writer, @Nullable final Resource config,
                             @Nullable final AuthorizablesView.NodeUrlBuilder nodeUrlBuilder,
                             @Nullable final AuthorizablesView.PathUrlBuilder pathUrlBuilder)
            throws IOException {
        writer.append("<table class=\"composum-nodes-usermgr-paths_table table-striped table-condensed\">\n  <tbody>\n");
        for (AuthorizableModel node : nodes.values()) {
            AuthorizableAcls acls = new AuthorizableAcls(resolver, node.getId());
            Map<String, AuthorizableAcls.AcRuleSet> affectedPaths = acls.getAffectedPaths();
            Collection<String> pathSet = filterPaths(affectedPaths.keySet());
            if (pathSet.size() > 0 || this.textPattern == null) {
                Iterator<String> pathIterator = pathSet.iterator();
                writer.append("    <tr class=\"").append(getNodeClass(node)).append("\">\n");
                writer.append("      <td class=\"authorizable-id\" rowspan=\"")
                        .append(Integer.toString(Math.max(1, pathSet.size())))
                        .append("\"><i class=\"icon fa fa-").append(node.getTypeIcon()).append("\"></i>");
                String url = null;
                if (nodeUrlBuilder != null && StringUtils.isNotBlank(url = nodeUrlBuilder.buildUrl(node))) {
                    writer.append("<a href=\"").append(url).append("\" data-path=\"").append(node.getPath()).append("\">");
                }
                writer.append(node.getId());
                if (url != null) {
                    writer.append("</a>");
                }
                writer.append("</td>\n      ");
                String path = pathIterator.hasNext() ? pathIterator.next() : null;
                writePath(writer, node, pathUrlBuilder, path, path != null ? affectedPaths.get(path) : null, true);
                while (pathIterator.hasNext()) {
                    path = pathIterator.next();
                    writePath(writer, node, pathUrlBuilder, path, affectedPaths.get(path), false);
                }
                writer.append("    </tr>\n");
            }
        }
        writer.append("  </tbody>\n</table>\n");
    }

    protected void writePath(@NotNull final Writer writer, @NotNull final AuthorizableModel node,
                             @Nullable final AuthorizablesView.PathUrlBuilder pathUrlBuilder,
                             @Nullable final String path, @Nullable final AuthorizableAcls.AcRuleSet acRuleSet,
                             boolean firstLine)
            throws IOException {
        if (!firstLine) {
            writer.append("    </tr>\n");
            writer.append("    <tr class=\"").append(getNodeClass(node)).append("\">\n      ");
        }
        writer.append("<td class=\"affected-path");
        if (path == null) {
            writer.append(" empty").append("\" colspan=\"2\"");
        }
        writer.append("\">");
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
        if (path != null && acRuleSet != null) {
            writeRules(writer, acRuleSet);
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
