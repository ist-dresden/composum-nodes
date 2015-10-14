package com.composum.sling.core.script;

import com.composum.sling.core.util.ResourceUtil;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rw on 12.10.15.
 */
@Component(
        label = "Groovy Execution Service",
        description = "Provides the execution of groovy scripts in the repository context.",
        immediate = true,
        metatype = true
)
@Service
public class GroovyServiceImpl implements GroovyService {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyServiceImpl.class);

    protected Map<String, ScriptJob> runningJobs;

    protected class ScriptJob implements Runnable {

        public final String key;
        public final Session session;
        public final Resource script;

        protected JobState state;
        protected StringWriter outBuffer;
        protected PrintWriter out;
        protected GroovyRunner runner;
        protected Thread thread;

        public ScriptJob(String key, Session session, Resource script) {
            this.key = key;
            this.session = session;
            this.script = script;
            outBuffer = new StringWriter();
            out = new PrintWriter(outBuffer);
            runner = new GroovyRunner(session, out);
            state = JobState.initialized;
        }

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        public void stop() {
            if (thread != null) {
                state = JobState.aborted;
                thread.stop();
                thread = null;
            }
        }

        public void run() {
            state = JobState.starting;
            try {
                Binary binary = ResourceUtil.getBinaryData(script);
                if (binary != null) {
                    try {
                        InputStream in = binary.getStream();
                        Reader reader = new InputStreamReader(in, "UTF-8");
                        state = JobState.running;
                        try {
                            runner.run(reader, null);
                            state = JobState.finished;
                        } catch (Exception ex) {
                            LOG.error(ex.getMessage(), ex);
                            state = JobState.error;
                            out.write("\n");
                            out.write(ex.getMessage());
                            out.write("\n");
                            ex.printStackTrace(out);
                        }
                    } catch (UnsupportedEncodingException ueex) {
                        LOG.error(ueex.getMessage(), ueex);
                        state = JobState.error;
                    } catch (RepositoryException rex) {
                        LOG.error(rex.getMessage(), rex);
                        state = JobState.error;
                    }
                } else {
                    state = JobState.error;
                    out.write("can't load script: " + script.getPath());
                }
            } finally {
                thread = null;
            }
        }

        public void flush(Writer out) throws IOException {
            StringBuffer buffer = this.outBuffer.getBuffer();
            synchronized (buffer) {
                this.out.flush();
                out.write(buffer.toString());
                buffer.delete(0, buffer.length());
            }
        }
    }

    public JobState startScript(String key, Resource script, PrintWriter out)
            throws IOException {
        ResourceResolver resolver = script.getResourceResolver();
        Session session = resolver.adaptTo(Session.class);
        ScriptJob scriptJob = new ScriptJob(key, session, script);
        addJob(scriptJob);
        switch (scriptJob.state) {
            case starting:
                scriptJob.start();
                scriptJob.flush(out);
                break;
        }
        return scriptJob.state;
    }

    public JobState checkScript(String key, PrintWriter out) throws IOException {
        ScriptJob scriptJob = getJob(key);
        if (scriptJob != null) {
            switch (scriptJob.state) {
                case finished:
                case aborted:
                case error:
                    dropJob(key);
                    break;
            }
            scriptJob.flush(out);
            return scriptJob.state;
        }
        return JobState.unknown;
    }

    public JobState stopScript(String key, PrintWriter out) throws IOException {
        ScriptJob scriptJob = getJob(key);
        if (scriptJob != null) {
            switch (scriptJob.state) {
                case finished:
                case aborted:
                case error:
                    dropJob(key);
                    break;
                default:
                    scriptJob.stop();
                    scriptJob.out.write("\nscript execution stopped!");
                    break;
            }
            scriptJob.flush(out);
            return scriptJob.state;
        }
        return JobState.unknown;
    }

    protected ScriptJob getJob(String key) {
        synchronized (runningJobs) {
            return runningJobs.get(key);
        }
    }

    protected void addJob(ScriptJob scriptJob) {
        synchronized (runningJobs) {
            ScriptJob existingJob = runningJobs.get(scriptJob.key);
            if (existingJob != null) {
                switch (existingJob.state) {
                    case finished:
                    case aborted:
                    case error:
                        runningJobs.remove(scriptJob.key);
                        break;
                }
            }
            runningJobs.put(scriptJob.key, scriptJob);
            scriptJob.state = JobState.starting;
        }
    }

    protected ScriptJob dropJob(String key) {
        ScriptJob scriptJob = null;
        synchronized (runningJobs) {
            scriptJob = runningJobs.get(key);
            if (scriptJob != null) {
                runningJobs.remove(key);
            }
        }
        return scriptJob;
    }

    @Activate
    protected void activate(ComponentContext ctx) throws Exception {
        runningJobs = new HashMap<>();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) throws Exception {
        synchronized (runningJobs) {
            for (Map.Entry<String, ScriptJob> entry : runningJobs.entrySet()) {
                entry.getValue().stop();
            }
            runningJobs.clear();
        }
    }
}