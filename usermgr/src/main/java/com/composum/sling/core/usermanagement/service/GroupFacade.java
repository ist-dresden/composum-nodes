package com.composum.sling.core.usermanagement.service;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import java.security.Principal;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class GroupFacade implements Group {

    protected final Authorizable delegate;

    public GroupFacade(@NotNull final Authorizable delegate) {
        this.delegate = delegate;
    }

    @Override
    public Iterator<Authorizable> getDeclaredMembers() throws RepositoryException {
        return delegate instanceof Group ? ((Group) delegate).getDeclaredMembers() : Collections.emptyIterator();
    }

    @Override
    public Iterator<Authorizable> getMembers() throws RepositoryException {
        return delegate instanceof Group ? ((Group) delegate).getMembers() : Collections.emptyIterator();
    }

    @Override
    public boolean isDeclaredMember(Authorizable authorizable) throws RepositoryException {
        return delegate instanceof Group && ((Group) delegate).isDeclaredMember(authorizable);
    }

    @Override
    public boolean isMember(Authorizable authorizable) throws RepositoryException {
        return delegate instanceof Group && ((Group) delegate).isMember(authorizable);
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
    public Iterator<Group> declaredMemberOf() throws RepositoryException {
        return delegate.declaredMemberOf();
    }

    @Override
    public Iterator<Group> memberOf() throws RepositoryException {
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
    public String getPath() throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegate.getPath();
    }
}
