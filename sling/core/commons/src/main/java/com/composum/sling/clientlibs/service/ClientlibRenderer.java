package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.processor.RendererContext;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public interface ClientlibRenderer {

    void renderClientlibLinks(Clientlib clientlib, Map<String, String> properties,
                              Writer writer, RendererContext context)
            throws IOException;
}
