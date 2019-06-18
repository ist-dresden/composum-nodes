package com.composum.sling.clientlibs.handle;

import com.composum.sling.core.util.LinkUtil;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Models a reference of a client-library to another client library, a category of client libraries, or a specific file.
 * <p>
 * Unfortunately we cannot define something like {@link ClientlibLink.Kind} here, since there is no way to distinguish
 * between a clientlib and a file at this point.
 */
public class ClientlibRef {

    /** Prefix for a pseudo-path that refers to all client libraries with {@link Clientlib#PROP_CATEGORY}. */
    public static final String PREFIX_CATEGORY = "category:";

    /**
     * Original description how to locate the corresponding resource: absolute / relative path to clientlib / file,
     * category, URI.
     */
    public final String rule;

    public final boolean optional;
    public final Pattern pattern;
    public final String path;
    public final String category;
    public final String externalUri;
    public final Clientlib.Type type;
    /** Additional properties, e.g. {@link ClientlibLink#PROP_REL}. */
    public final Map<String, String> properties;

    public ClientlibRef(Clientlib.Type type, String theRule, boolean optional, Map<String, String> properties) {
        this.type = type;
        this.rule = theRule.trim();
        this.optional = optional;
        boolean isCategory = rule.startsWith(PREFIX_CATEGORY);
        boolean isUri = !isCategory && LinkUtil.isExternalUrl(rule);
        this.externalUri = isUri ? rule : null;
        this.pattern = (!isUri && !isCategory) ? ruleToPattern(rule) : null;
        this.path = (!isUri && !isCategory) ? ruleToPath(rule) : null;
        this.category = isCategory ? rule.substring(PREFIX_CATEGORY.length()) : null;
        this.properties = Collections.unmodifiableMap(properties != null ? new HashMap<>(properties) : new
                HashMap<>());
    }

    public static ClientlibRef forCategory(Clientlib.Type type, String category, boolean optional, Map<String,
            String> properties) {
        return new ClientlibRef(type, PREFIX_CATEGORY + category, optional, properties);
    }

    public boolean isCategory() {
        return null != category;
    }

    public boolean isExternalUri() {
        return null != externalUri;
    }

    public boolean getExternalUri() {
        return null != externalUri;
    }

    /** Checks whether the link matches this, up to version patterns. */
    public boolean isSatisfiedby(ClientlibLink link) {
        if (!type.equals(link.type)) return false;
        if (isCategory()) {
            if (!link.isCategory() || !category.equals(link.path)) return false;
        } else if (isExternalUri()) {
            if (!link.isExternalUri() || !externalUri.equals(link.path)) return false;
        } else {
            if (link.isCategory() || link.isExternalUri() || !pattern.matcher(link.path).matches()) return false;
        }
        return properties.equals(link.properties);
    }

    /** Checks whether one of the links matches this, up to version patterns. */
    public boolean isSatisfiedby(Collection<ClientlibLink> links) {
        for (ClientlibLink link : links) if (isSatisfiedby(link)) return true;
        return false;
    }

    protected Pattern ruleToPattern(String rule) {
        // (xxx*:yyy) -> (xxx[^/]*:yyy)
        rule = rule.replaceAll("(\\([^:]+)\\*(:[^)]+\\))", "$1[^/]*$2");
        // (xxxx:yyy) -> xxxx
        rule = rule.replaceAll("\\(([^:]+):[^)]+\\)", "($1)");
        // make '.min' optional, or insert it if not present, to match both minified and unminified files.
        rule = rule.replaceFirst("([.-]min)?(\\.[^./]+)?$", "(.min)?$2");
        // check dots as dots
        rule = rule.replaceAll("\\.", "\\\\.");
        // allow code paths at start
        rule = "^" + (!rule.startsWith("/") ? ".*/" : "") + rule + "$";
        return Pattern.compile(rule);
    }

    protected String ruleToPath(final String rule) {
        if (rule.startsWith(PREFIX_CATEGORY)) return rule;
        // (xxx:yyy) -> yyy
        return rule.replaceAll("\\([^:]+:([^)]+)\\)", "$1");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(type).append(':').append(rule);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            builder.append(";").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientlibRef)) return false;

        ClientlibRef that = (ClientlibRef) o;

        if (!Objects.equals(rule, that.rule)) return false;
        if (!Objects.equals(category, that.category)) return false;
        if (!Objects.equals(externalUri, that.externalUri)) return false;
        if (type != that.type) return false;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        int result = rule != null ? rule.hashCode() : 0;
        result = 92821 * result + (category != null ? category.hashCode() : 0);
        result = 92821 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

}
