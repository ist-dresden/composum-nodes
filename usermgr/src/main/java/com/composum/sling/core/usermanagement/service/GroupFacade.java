package com.composum.sling.core.usermanagement.service;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.security.Principal;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * represents an authorizable as a group; used to construct a service user as a member of the assigned system users
 */
public class GroupFacade extends GroupWrapper {

    protected final AuthorizableWrapper delegate;
    protected final ServiceUserWrapper serviceUser;

    public GroupFacade(@NotNull final AuthorizableWrapper delegate, @NotNull final ServiceUserWrapper serviceUser) {
        super(null);
        this.delegate = delegate;
        this.serviceUser = serviceUser;
    }

    @Override
    public Iterator<AuthorizableWrapper> getDeclaredMembers() throws RepositoryException {
        return delegate instanceof GroupWrapper
                ? ((GroupWrapper) delegate).getDeclaredMembers()
                : Collections.singleton((AuthorizableWrapper) serviceUser).iterator();
    }

    @Override
    public Iterator<AuthorizableWrapper> getMembers() throws RepositoryException {
        return delegate instanceof GroupWrapper
                ? ((GroupWrapper) delegate).getMembers()
                : Collections.singleton((AuthorizableWrapper) serviceUser).iterator();
    }

    @Override
    public boolean isDeclaredMember(Authorizable authorizable) throws RepositoryException {
        return authorizable.equals(serviceUser) ||
                (delegate instanceof Group && ((Group) delegate).isDeclaredMember(authorizable));
    }

    @Override
    public boolean isMember(Authorizable authorizable) throws RepositoryException {
        return authorizable.equals(serviceUser) ||
                (delegate instanceof Group && ((Group) delegate).isMember(authorizable));
    }

    @Override
    public boolean addMember(Authorizable authorizable) throws RepositoryException {
        return delegate instanceof Group && ((Group) delegate).addMember(authorizable);
    }

    @Override
    public Set<String> addMembers(@NotNull String... memberIds) throws RepositoryException {
        return delegate instanceof Group ? ((Group) delegate).addMembers(memberIds) : Collections.emptySet();
    }

    @Override
    public boolean removeMember(Authorizable authorizable) throws RepositoryException {
        return delegate instanceof Group && ((Group) delegate).removeMember(authorizable);
    }

    @Override
    public Set<String> removeMembers(@NotNull String... memberIds) throws RepositoryException {
        return delegate instanceof Group ? ((Group) delegate).removeMembers(memberIds) : Collections.emptySet();
    }

    @Override
    public String getID() throws RepositoryException {
        return delegate.getID();
    }

    @Override
    public boolean isGroup() {
        return delegate.isGroup();
    }

    @Override
    public Principal getPrincipal() throws RepositoryException {
        return delegate.getPrincipal();
    }

    @Override
    public Iterator<GroupWrapper> declaredMemberOf() throws RepositoryException {
        return delegate.declaredMemberOf();
    }

    @Override
    public Iterator<GroupWrapper> memberOf() throws RepositoryException {
        return delegate.memberOf();
    }

    @Override
    public void remove() throws RepositoryException {
        delegate.remove();
    }

    @Override
    public Iterator<String> getPropertyNames() throws RepositoryException {
        return delegate.getPropertyNames();
    }

    @Override
    public Iterator<String> getPropertyNames(String relPath) throws RepositoryException {
        return delegate.getPropertyNames(relPath);
    }

    @Override
    public boolean hasProperty(String relPath) throws RepositoryException {
        return delegate.hasProperty(relPath);
    }

    @Override
    public void setProperty(String relPath, Value value) throws RepositoryException {
        delegate.setProperty(relPath, value);
    }

    @Override
    public void setProperty(String relPath, Value[] value) throws RepositoryException {
        delegate.setProperty(relPath, value);
    }

    @Override
    public Value[] getProperty(String relPath) throws RepositoryException {
        return delegate.getProperty(relPath);
    }

    @Override
    public boolean removeProperty(String relPath) throws RepositoryException {
        return delegate.removeProperty(relPath);
    }

    @Override
    public String getPath() throws RepositoryException {
        return delegate.getPath();
    }
}
