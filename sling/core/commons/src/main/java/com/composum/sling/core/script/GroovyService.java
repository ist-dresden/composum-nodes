package com.composum.sling.core.script;


import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.Writer;

public interface GroovyService {

    enum JobState {initialized, starting, running, finished, aborted, error, unknown}

    interface Job {

        JobState getState();

        void flush(Writer out) throws IOException;
    }

    Job startScript(String key, Session session, String scriptPath) throws IOException, RepositoryException;

    Job checkScript(String key) throws IOException;

    Job stopScript(String key) throws IOException;
}