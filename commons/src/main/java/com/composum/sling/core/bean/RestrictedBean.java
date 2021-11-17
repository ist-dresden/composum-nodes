package com.composum.sling.core.bean;

public interface RestrictedBean {

    boolean isReadAllowed();

    boolean isWriteAllowed();
}
