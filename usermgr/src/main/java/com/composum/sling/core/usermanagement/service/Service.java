package com.composum.sling.core.usermanagement.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.serviceusermapping.Mapping;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.security.Principal;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class Service implements Authorizable {

    public static final String SERVICE_USER_ROOT = "/home/service";

    public class ServicePrincipal implements Principal {

        @Override
        public String getName() {
            return getID();
        }
    }

    protected final String id;
    protected final String path;
    protected final Mapping mapping;
    protected final Principal principal;
    protected final Set<Group> declaredMemberOf;
    protected final Set<Group> memberOf;

    public Service(Authorizables.Context context, Mapping mapping) throws RepositoryException {
        this.mapping = mapping;
        String serviceName = mapping.getServiceName();
        String serviceInfo = mapping.getSubServiceName();
        if (StringUtils.isNotBlank(serviceInfo)) {
            id = serviceName + ":" + serviceInfo;
            path = SERVICE_USER_ROOT + "/" + serviceName.replace('.', '/') + "/" + serviceInfo;
        } else {
            id = serviceName;
            path = SERVICE_USER_ROOT + "/" + serviceName.replace('.', '/');
        }
        principal = new ServicePrincipal();
        declaredMemberOf = new LinkedHashSet<>();
        memberOf = new LinkedHashSet<>();
        Authorizable authorizable;
        String user = mapping.map(mapping.getServiceName(), mapping.getSubServiceName());
        if (StringUtils.isNotBlank(user)) {
            authorizable = context.getService().getAuthorizable(context, user);
            if (authorizable != null) {
                declaredMemberOf.add(new GroupFacade(authorizable));
            }
        }
        Iterable<String> principals = mapping.mapPrincipals(mapping.getServiceName(), mapping.getSubServiceName());
        if (principals != null) {
            for (String principal : principals) {
                authorizable = context.getService().getAuthorizable(context, user);
                if (authorizable != null) {
                    declaredMemberOf.add(new GroupFacade(authorizable));
                }
            }
        }
        for (Group group : declaredMemberOf) {
            memberOf.add(group);
            Iterator<Group> groups = group.memberOf();
            while (groups.hasNext()) {
                memberOf.add(groups.next());
            }
        }
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Iterator<Group> declaredMemberOf() {
        return declaredMemberOf.iterator();
    }

    @Override
    public Iterator<Group> memberOf() {
        return memberOf.iterator();
    }

    @Override
    public void remove() throws RepositoryException {
    }

    @Override
    public Iterator<String> getPropertyNames() {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<String> getPropertyNames(String relPath) {
        return Collections.emptyIterator();
    }

    @Override
    public boolean hasProperty(String relPath) {
        return false;
    }

    @Override
    public void setProperty(String relPath, Value value) {

    }

    @Override
    public void setProperty(String relPath, Value[] value) {

    }

    @Override
    public Value[] getProperty(String relPath) {
        return new Value[0];
    }

    @Override
    public boolean removeProperty(String relPath) {
        return false;
    }

    @Override
    public String getPath() {
        return path;
    }
}
