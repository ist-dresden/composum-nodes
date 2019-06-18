package com.composum.sling.core.util;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;

/** Some Composum core-wide constants - mostly node types, property names etc. */
public interface CoreConstants extends JcrConstants {

    /** sling:resourceType */
    String PROP_RESOURCE_TYPE =
            SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE;
    /** sling:resourceSuperType */
    String PROP_RESOURCE_SUPER_TYPE =
            SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE;
    /** jcr:content */
    String CONTENT_NODE = JCR_CONTENT;

    /** mix:created */
    String MIX_CREATED = "mix:created";
    /** mix:lastModified */
    String MIX_LAST_MODIFIED = "mix:lastModified";
    /** mix:title */
    String MIX_TITLE = "mix:title";

    /** jcr:title */
    String JCR_TITLE = "jcr:title";
    /** jcr:description */
    String JCR_DESCRIPTION = "jcr:description";

    String TYPE_OAKINDEX = "oak:QueryIndexDefinition";
    String TYPE_FOLDER = NT_FOLDER;
    String TYPE_FILE = NT_FILE;
    String TYPE_LINKED_FILE = NT_LINKEDFILE;
    String TYPE_RESOURCE = NT_RESOURCE;
    String TYPE_UNSTRUCTURED = NT_UNSTRUCTURED;

    String TYPE_SLING_RESOURCE = "sling:Resource";
    String TYPE_SLING_FOLDER = "sling:Folder";
    String TYPE_SLING_ORDERED_FOLDER = "sling:OrderedFolder";

    String TYPE_LOCKABLE = MIX_LOCKABLE;
    String TYPE_REFERENCEABLE = MIX_REFERENCEABLE;
    String TYPE_LAST_MODIFIED = MIX_LAST_MODIFIED;
    String TYPE_CREATED = MIX_CREATED;
    String TYPE_TITLE = MIX_TITLE;
    String TYPE_VERSIONABLE = MIX_VERSIONABLE;

    String PROP_UUID = JCR_UUID;
    String PROP_TITLE = JCR_TITLE;
    String PROP_DESCRIPTION = JCR_DESCRIPTION;

    String PROP_DATA = JCR_DATA;
    String PROP_MIME_TYPE = JCR_MIMETYPE;
    String PROP_ENCODING = JCR_ENCODING;
    String PROP_PRIMARY_TYPE = JCR_PRIMARYTYPE;
    String PROP_MIXINTYPES = JCR_MIXINTYPES;
    String PROP_JCR_CONTENT = JCR_CONTENT;
    String PROP_CREATED = JCR_CREATED;
    String PROP_LAST_MODIFIED = JCR_LASTMODIFIED;
    String PROP_FILE_REFERENCE = "fileReference";

    String JCR_LASTMODIFIED_BY = "jcr:lastModifiedBy";
    String JCR_CREATED_BY = "jcr:createdBy";
}
