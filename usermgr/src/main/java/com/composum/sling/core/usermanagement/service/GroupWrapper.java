package com.composum.sling.core.usermanagement.service;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.Set;

/**
 * Group wrapper, Authorizable interface is not a ConsumerType and so should not be implemented. See OAK-10252
 */
public class GroupWrapper extends AuthorizableWrapper {
    private final Group group;

    public GroupWrapper(Group group) {
        super(group);
        this.group = group;
    }

    public Iterator<AuthorizableWrapper> getDeclaredMembers() throws RepositoryException {
        return getAuthorizableWrapperIterator(group.getDeclaredMembers());
    }

    public Iterator<AuthorizableWrapper> getMembers() throws RepositoryException {
        return getAuthorizableWrapperIterator(group.getMembers());
    }

    public boolean isDeclaredMember(Authorizable authorizable) throws RepositoryException {
        return group.isDeclaredMember(authorizable);
    }

    public boolean isMember(Authorizable authorizable) throws RepositoryException {
        return group.isMember(authorizable);
    }

    public boolean addMember(Authorizable authorizable) throws RepositoryException {
        return group.addMember(authorizable);
    }

    public Set<String> addMembers(@NotNull String... memberIds) throws RepositoryException {
        return group.addMembers(memberIds);
    }

    public boolean removeMember(Authorizable authorizable) throws RepositoryException {
        return group.removeMember(authorizable);
    }

    public Set<String> removeMembers(@NotNull String... memberIds) throws RepositoryException {
        return group.removeMembers(memberIds);
    }
}
