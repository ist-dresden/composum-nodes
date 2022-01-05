package com.composum.sling.core.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ProviderType
public interface ServiceRestrictions {

    String SA_PERMISSION = ServiceRestrictions.class.getName() + "#permission";

    enum Permission {
        none, read, write;

        public boolean matches(Permission required) {
            return this.compareTo(required) >= 0;
        }
    }

    class Key {

        public static final char SEPARATOR = '/';

        public final List<String> identifier;

        public Key(@NotNull String rule) {
            while (rule.startsWith("" + SEPARATOR)) {
                rule = rule.substring(1);
            }
            identifier = StringUtils.isNotBlank(rule)
                    ? Arrays.asList(StringUtils.split(rule, SEPARATOR))
                    : Collections.emptyList();
        }

        public boolean isEmpty() {
            return identifier.size() == 0;
        }

        @Override
        public String toString() {
            return StringUtils.join(identifier, SEPARATOR);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key && toString().equals(other.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }

    class Restriction {

        public static final String SEPARATOR = ":";

        public final Permission permission;
        public final String restrictions;

        public Restriction(@NotNull final String rule) {
            final String[] splitted = rule.split(SEPARATOR, 2);
            Permission perm = null;
            if (StringUtils.isNotBlank(splitted[0])) {
                try {
                    perm = Permission.valueOf(splitted[0]);
                } catch (IllegalArgumentException ignore) {
                }
            }
            permission = perm;
            restrictions = splitted.length > 1 ? splitted[1] : null;
        }

        public Restriction() {
            permission = null;
            restrictions = null;
        }

        @Override
        public String toString() {
            return (permission != null ? permission.name() : "")
                    + (StringUtils.isNotBlank(restrictions) ? (SEPARATOR + restrictions) : "");
        }
    }

    boolean isUserOptionAllowed(@NotNull SlingHttpServletRequest request, @NotNull Permission permission);

    Permission getDefaultPermisson();

    boolean isPermissible(@Nullable SlingHttpServletRequest request, @Nullable Key key, @NotNull Permission needed);

    @NotNull
    Permission getPermission(@Nullable Key key);

    @Nullable
    String getRestrictions(@Nullable Key key);
}
