package com.composum.sling.nodes.browser;

import org.apache.sling.api.resource.ResourceResolver;

public interface MergeMountpointService {

    /**
     * The position of the Sling resource type hierarchy based resource merger , normally /mnt/override .
     */
    String overrideMergeMountPoint(ResourceResolver resolver);

    /**
     * The position of the Sling search based resource picker, normally /mnt/overlay .
     */
    String overlayMergeMountPoint(ResourceResolver resolver);
}
