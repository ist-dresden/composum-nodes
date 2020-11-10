package com.composum.sling.clientlibs.processor;

import com.composum.sling.clientlibs.service.ClientlibProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    public InputStream processContent(InputStream stream, ProcessorContext context)
            throws IOException {
        for (ClientlibProcessor processor : processors) {
            stream = processor.processContent(stream, context);
        }
        return stream;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("ProcessorPipeline{");
        if (processors != null) {
            for (ClientlibProcessor processor : processors) {
                buf.append(processor.toString());
            }
        }
        buf.append("}");
        return buf.toString();
    }
}
