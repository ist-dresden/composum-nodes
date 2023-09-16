package com.composum.sling.core.usermanagement.service;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.jetbrains.annotations.NotNull;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Authorizable wrapper, Authorizable interface is not a ConsumerType and so should not be implemented. See OAK-10252
 */
public class AuthorizableWrapper {

    private final Authorizable authorizable;

    public AuthorizableWrapper(Authorizable authorizable) {
        this.authorizable = authorizable;
    }

    public String getID() throws RepositoryException {
        return authorizable.getID();
    }

    public boolean isGroup() {
        return authorizable.isGroup();
    }

    public Principal getPrincipal() throws RepositoryException {
        return authorizable.getPrincipal();
    }

    public Iterator<GroupWrapper> declaredMemberOf() throws RepositoryException {
        Iterator<Group> jcrGroupIterator = authorizable.declaredMemberOf();
        return getGroupWrapperIterator(jcrGroupIterator);
    }

    public Iterator<GroupWrapper> memberOf() throws RepositoryException {
        Iterator<Group> jcrGroupIterator = authorizable.memberOf();
        return getGroupWrapperIterator(jcrGroupIterator);
    }

    public void remove() throws RepositoryException {
        authorizable.remove();
    }

    public Iterator<String> getPropertyNames() throws RepositoryException {
        return authorizable.getPropertyNames();
    }

    public Iterator<String> getPropertyNames(String relPath) throws RepositoryException {
        return authorizable.getPropertyNames(relPath);
    }

    public boolean hasProperty(String relPath) throws RepositoryException {
        return authorizable.hasProperty(relPath);
    }

    public void setProperty(String relPath, Value value) throws RepositoryException {
        authorizable.setProperty(relPath, value);
    }

    public void setProperty(String relPath, Value[] value) throws RepositoryException {
        authorizable.setProperty(relPath, value);
    }

    public Value[] getProperty(String relPath) throws RepositoryException {
        return authorizable.getProperty(relPath);
    }

    public boolean removeProperty(String relPath) throws RepositoryException {
        return authorizable.removeProperty(relPath);
    }

    public String getPath() throws UnsupportedRepositoryOperationException, RepositoryException {
        return authorizable.getPath();
    }

    @NotNull
    protected static Iterator<GroupWrapper> getGroupWrapperIterator(Iterator<Group> jcrGroupIterator) {
        List<GroupWrapper> groupWrapperList = new ArrayList<>();
        while (jcrGroupIterator.hasNext()) {
            groupWrapperList.add(new GroupWrapper(jcrGroupIterator.next()));
        }
        return groupWrapperList.iterator();
    }

    @NotNull
    protected static Iterator<AuthorizableWrapper> getAuthorizableWrapperIterator(Iterator<Authorizable> jcrAuthorizableIterator) {
        List<AuthorizableWrapper> authorizableWrapperList = new ArrayList<>();
        while (jcrAuthorizableIterator.hasNext()) {
            authorizableWrapperList.add(new AuthorizableWrapper(jcrAuthorizableIterator.next()));
        }
        return authorizableWrapperList.iterator();
    }

    public Authorizable getAuthorizable() {
        return authorizable;
    }
}
