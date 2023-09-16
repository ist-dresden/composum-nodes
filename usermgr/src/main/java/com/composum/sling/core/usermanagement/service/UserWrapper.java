package com.composum.sling.core.usermanagement.service;

import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

/**
 * User wrapper, Authorizable interface is not a ConsumerType and so should not be implemented. See OAK-10252
 */
public class UserWrapper extends AuthorizableWrapper {

    private final User user;

    public UserWrapper(User user) {
        super(user);
        this.user = user;
    }

    public boolean isAdmin() {
        return user.isAdmin();
    }

    public boolean isSystemUser() {
        return user.isSystemUser();
    }

    public Credentials getCredentials() throws RepositoryException {
        return user.getCredentials();
    }

    public Impersonation getImpersonation() throws RepositoryException {
        return user.getImpersonation();
    }

    public void changePassword(String password) throws RepositoryException {
        user.changePassword(password);
    }

    public void changePassword(String password, String oldPassword) throws RepositoryException {
        user.changePassword(password, oldPassword);
    }

    public void disable(String reason) throws RepositoryException {
        user.disable(reason);
    }

    public boolean isDisabled() throws RepositoryException {
        return user.isDisabled();
    }

    public String getDisabledReason() throws RepositoryException {
        return user.getDisabledReason();
    }
}
