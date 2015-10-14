package com.composum.sling.core.script;


import org.apache.sling.api.resource.Resource;

import java.io.IOException;
import java.io.PrintWriter;

public interface GroovyService {

    enum JobState {initialized, starting, running, finished, aborted, error, unknown}

    JobState startScript(String key, Resource script, PrintWriter out) throws IOException;

    JobState checkScript(String key, PrintWriter out) throws IOException;

    JobState stopScript(String key, PrintWriter out) throws IOException;
}