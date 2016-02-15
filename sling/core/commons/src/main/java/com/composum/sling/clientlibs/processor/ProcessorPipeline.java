package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.service.ClientlibProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessorPipeline implements ClientlibProcessor {

    protected List<ClientlibProcessor> processors;

    public ProcessorPipeline(ClientlibProcessor... processors) {
        this.processors = new ArrayList<>();
        for (ClientlibProcessor processor : processors) {
            addProcessor(processor);
        }
    }

    public void addProcessor(ClientlibProcessor processor) {
        if (processor != null) {
            processors.add(processor);
        }
    }

    public void addProcessor(int index, ClientlibProcessor processor) {
        if (processor != null) {
            if (index < 0) index = 0;
            if (index > processors.size()) index = processors.size();
            processors.add(index, processor);
        }
    }

    @Override
    public InputStream processContent(final Clientlib clientlib, InputStream stream, Map<String,Object> hints)
            throws IOException {
        for (ClientlibProcessor processor : processors) {
            stream = processor.processContent(clientlib, stream, hints);
        }
        return stream;
    }
}
