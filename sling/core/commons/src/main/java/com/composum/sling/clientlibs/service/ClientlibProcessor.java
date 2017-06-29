package com.composum.sling.clientlibs.service;

import com.composum.sling.clientlibs.processor.ProcessorContext;

import java.io.IOException;
import java.io.InputStream;

/** Modifies the content of resources embedded in a client library. */
public interface ClientlibProcessor {

    String DEFAULT_CHARSET = "UTF-8";

    /**
     * Transforms the content and result in a stream with the probably changed content - for pipes.
     *
     * @param sourceStream the original to process by the processor
     * @return the transformation result - might also be the originial sourceStream if switched off
     */
    InputStream processContent(InputStream sourceStream, ProcessorContext context)
            throws IOException;
}
