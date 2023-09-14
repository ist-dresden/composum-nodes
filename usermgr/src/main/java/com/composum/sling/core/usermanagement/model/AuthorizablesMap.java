package com.composum.sling.core.usermanagement.model;

import com.composum.sling.core.usermanagement.service.Authorizables;
import com.google.gson.stream.JsonWriter;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AuthorizablesMap extends AuthorizablesView {

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

    protected final Set<Relation> targetRelations = new HashSet<>();
    protected final Set<Relation> sourceRelations = new HashSet<>();

    public AuthorizablesMap(@NotNull final Authorizables.Context context,
                            @Nullable final Class<? extends Authorizable> selector,
                            @Nullable final String nameQueryPattern,
                            @Nullable final Authorizables.Filter filter)
            throws RepositoryException {
        super(context, selector, nameQueryPattern, filter);
        scanRelations(selector, filter);
    }

    protected void scanRelations(@Nullable final Class<? extends Authorizable> selector,
                                 @Nullable final Authorizables.Filter filter)
            throws RepositoryException {
        Set<String> singleFocusDone = singleFocus != null ? new HashSet<>() : null;
        for (AuthorizableModel source : nodes.values()) {
            addTargetRelations(source, singleFocusDone);
        }
        extendedScan(selector, filter, singleFocusDone);
    }

    protected void extendedScan(@Nullable final Class<? extends Authorizable> selector,
                                @Nullable final Authorizables.Filter filter,
                                @Nullable final Set<String> singleFocusDone)
            throws RepositoryException {
    }

    protected void addTargetRelations(@NotNull final AuthorizableModel source, @Nullable final Set<String> done)
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
                target = addNode(context.getService().getAuthorizable(context, targetId));
            }
            if (target != null) {
                targetRelations.add(new AuthorizablesGraph.Relation(source, target));
                if (done != null) {
                    addTargetRelations(target, done);
                }
            }
        }
    }

    protected void addSourceRelations(@Nullable final Class<? extends Authorizable> selector,
                                      @Nullable final Authorizables.Filter filter,
                                      @NotNull final AuthorizableModel target, @NotNull final Set<String> done)
            throws RepositoryException {
        Set<? extends Authorizable> sources = context.getService().findAuthorizables(
                context, selector, null, authorizable
                        -> (filter == null || filter.accept(authorizable))
                        && isSourceOfTarget(authorizable, target.getId()));
        for (Authorizable source : sources) {
            if (done.add(source.getID())) {
                sourceRelations.add(new AuthorizablesGraph.Relation(addNode(source), target));
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
}
