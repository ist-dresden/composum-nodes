package com.composum.sling.core.script;


import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.PrintWriter;

public interface GroovyService {

    enum JobState {initialized, starting, running, finished, aborted, error, unknown}

    JobState startScript(String key, Session session, String scriptPath, PrintWriter out) throws IOException, RepositoryException;

    JobState checkScript(String key, PrintWriter out) throws IOException;

    JobState stopScript(String key, PrintWriter out) throws IOException;
}