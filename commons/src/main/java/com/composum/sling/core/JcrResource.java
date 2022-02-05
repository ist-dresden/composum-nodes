package com.composum.sling.core;

/**
 * In a JCR context each resource should have a primary type and the resource should return this type.
 * This extended interface is used in a versioned context to determine the primary type of a versioned resource
 * by the staging resource wrapper itself.
 */
public interface JcrResource {

    String getPrimaryType();
}
