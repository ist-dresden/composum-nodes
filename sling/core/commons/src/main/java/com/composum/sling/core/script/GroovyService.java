package com.composum.sling.core.script;


import org.apache.sling.api.resource.Resource;

import javax.jcr.Session;
import java.io.PrintWriter;
import java.io.Reader;

public interface GroovyService {

    boolean startScript(String key, Resource script, PrintWriter out);

    boolean startScript(String key, Session session, Reader reader, PrintWriter out);

    boolean checkScript(String key, PrintWriter out);

    boolean stopScript(String key, PrintWriter out);
}