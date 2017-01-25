package com.composum.sling.clientlibs.handle;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * the reference declared in a clientlib configuration can contain rules to reference a range of
 * possible library versions which are (re)used if they are rendered already for the current page
 * TODO change rule implementation and use version rules like Maven version rules
 */
public class ClientlibRef extends ClientlibKey {

    public final Boolean depends;   // a dependency reference must have its own link
    public final boolean optional;

    protected final String rule;
    protected final Pattern pattern;

    protected ClientlibLink usedAlternative;

    /**
     * derive from another reference and change the flags
     */
    public ClientlibRef(final ClientlibRef referrer,
                        final Boolean depends, final boolean optional) {
        this(referrer.type, referrer.rule, depends, optional, new HashMap<String, String>());
    }

    /**
     * derive from another reference with a different rule
     */
    public ClientlibRef(final ClientlibRef referrer, final String rule,
                        final Boolean depends, final boolean optional) {
        this(referrer.type, rule, depends, optional, referrer.properties);
    }

    /**
     * derive from another reference with a different rule
     */
    public ClientlibRef(final ClientlibRef referrer, final String rule, final boolean optional) {
        this(referrer.type, rule, referrer.depends, optional, referrer.properties);
    }

    public ClientlibRef(final Clientlib.Type type, final String rule,
                        final Boolean depends, final boolean optional) {
        this(type, rule, depends, optional, new HashMap<String, String>());
    }

    public ClientlibRef(final Clientlib.Type type, final String rule,
                        final Boolean depends, final boolean optional,
                        Map<String, String> properties) {
        super(type, ruleToPath(rule), properties);
        this.rule = rule;
        pattern = ruleToPattern(rule);
        this.depends = depends;
        this.optional = optional;
    }

    public boolean isAlternativeUsed() {
        return getUsedAlternative() != null;
    }

    public ClientlibLink getUsedAlternative() {
        return usedAlternative;
    }

    /**
     * check an already present link for compatibility and use this link if possible
     */
    public boolean use(ClientlibLink link) {
        if (type == link.type && pattern.matcher(link.keyPath).matches()) {
            usedAlternative = link;
            return true;
        }
        return false;
    }

    // transform version rules like "jslibs/jquery/([1-3]*:3.1.1)/jquery.js"
    // to a regex pattern (e.g. "^.*/jslibs/jquery/[1-3][^/]*/jquery.js$")
    // and to a path to the preferred artifact (e.g. "jslibs/jquery/3.1.1)/jquery.js")

    protected Pattern ruleToPattern(String rule) {
        // (xxx*:yyy) -> (xxx[^/]*:yyy)
        rule = rule.replaceAll("(\\([^:]+)\\*(:[^)]+\\))", "$1[^/]*$2");
        // (xxxx:yyy) -> xxxx
        rule = rule.replaceAll("\\(([^:]+):[^)]+\\)", "$1");
        // make '.min' optional
        rule = rule.replaceAll("([\\.-]min)(\\.[^./]+)?$", "($1)?$2");
        // check dots as dots
        rule = rule.replaceAll("\\.", "\\\\.");
        // allow code paths at start
        rule = "^" + (!rule.startsWith("/") ? ".*/" : "") + rule + "$";
        return Pattern.compile(rule);
    }

    protected static String ruleToPath(final String rule) {
        // (xxx:yyy) -> yyy
        return rule.replaceAll("\\([^:]+:([^)]+)\\)", "$1");
    }
}
