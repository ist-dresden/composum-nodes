package com.composum.sling.clientlibs.handle;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

public class ImageHandle extends FileHandle {

    public ImageHandle(@NotNull Resource resource) {
        super(resource);
    }
}
