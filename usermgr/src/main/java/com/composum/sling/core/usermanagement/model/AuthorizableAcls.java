package com.composum.sling.core.usermanagement.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;

import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AuthorizableAcls {

    public enum AcType {Grant, Deny}

    public class AcRule {

        protected final AcType type;
        protected final String[] privileges;
        protected final Map<String, String[]> restrictions;

        public AcRule(@NotNull final AcType type, @NotNull final Resource aclResource) {
            this.type = type;
            ValueMap values = aclResource.getValueMap();
            privileges = values.get("rep:privileges", new String[0]);
            restrictions = new TreeMap<>();
            Resource restrictRes = aclResource.getChild("rep:restrictions");
            if (restrictRes != null) {
                for (Map.Entry<String, Object> entry : restrictRes.getValueMap().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key.startsWith("rep:") && value != null) {
                        restrictions.put(key, value instanceof String[]
                                ? ((String[]) value) : new String[]{value.toString()});
                    }
                }
            }
        }

        public AcType getType() {
            return type;
        }

        public String[] getPrivileges() {
            return privileges;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(type.name().toLowerCase());
            builder.append(": ").append(StringUtils.join(getPrivileges(), ", "));
            if (restrictions.size() > 0) {
                builder.append(" (");
                int count = 0;
                for (Map.Entry<String, String[]> entry : restrictions.entrySet()) {
                    if (count > 0) {
                        builder.append(",");
                    }
                    count++;
                    String[] value = entry.getValue();
                    builder.append(entry.getKey()).append("=").append(value.length > 0
                            ? (value.length > 1 ? ("[" + StringUtils.join(value, ",") + "]") : value[0]) : "");
                }
                builder.append(")");
            }
            return builder.toString();
        }
    }

    public class AcRuleSet {

        protected final List<AcRule> rules = new ArrayList<>();

        public List<AcRule> getRules() {
            return rules;
        }

        public void add(@NotNull final AcType type, @NotNull final Resource aclResource) {
            rules.add(new AcRule(type, aclResource));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (AcRule rule : rules) {
                if (builder.length() > 0) {
                    builder.append("; ");
                }
                builder.append(rule);
            }
            return builder.toString();
        }
    }

    protected final Map<String, AcRuleSet> affectedPaths = new TreeMap<>();

    public AuthorizableAcls(@NotNull final ResourceResolver resolver,
                            @NotNull final String authorizableId) {
        findACLs(resolver, authorizableId, AcType.Deny);
        findACLs(resolver, authorizableId, AcType.Grant);
    }

    public Map<String, AcRuleSet> getAffectedPaths() {
        return affectedPaths;
    }

    protected void addAcRule(@NotNull final AcType type,
                             @NotNull final String path, @NotNull final Resource aclResource) {
        AcRuleSet set = affectedPaths.get(path);
        if (set == null) {
            affectedPaths.put(path, set = new AcRuleSet());
        }
        set.add(type, aclResource);
    }

    protected void findACLs(@NotNull final ResourceResolver resolver,
                            @NotNull final String authorizableId, @NotNull final AcType type) {
        String query = "SELECT * FROM [rep:" + type.name() + "ACE] AS s WHERE  [rep:principalName] = '" + authorizableId + "'";
        Iterator<Resource> aclResources = resolver.findResources(query, Query.JCR_SQL2);
        while (aclResources.hasNext()) {
            Resource aclResource = aclResources.next();
            String path = aclResource.getPath();
            path = path.substring(0, path.indexOf("/rep:policy"));
            if (StringUtils.isBlank(path)) {
                path = "/";
            }
            addAcRule(type, path, aclResource);
        }
    }
}
