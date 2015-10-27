package com.composum.sling.core.script;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.util.PropertyUtil;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
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

    @Reference
    protected CoreConfiguration coreConfig;

    @Reference
    protected SlingRepository slingRepository;

    @Reference
    protected ThreadPoolManager threadPoolManager;

    protected ThreadPool threadPool;

    protected String setupScript;

    protected Map<String, ScriptJob> runningJobs;

    protected class ScriptJob implements Runnable {

        public final String key;
        public final String scriptPath;

        protected Session session;
        protected JobState state;
        protected StringWriter outBuffer;
        protected PrintWriter out;
        protected GroovyRunner runner;
        protected Thread thread;

        public ScriptJob(String key, Session session, String scriptPath) throws RepositoryException {
            this.key = key;
            this.scriptPath = scriptPath;
            // clone session for request independent execution; TODO avoid loginAdministrative ?...
            Session admin = slingRepository.loginAdministrative(session.getWorkspace().getName());
            this.session = admin.impersonate(new SimpleCredentials(session.getUserID(), new char[0]));
            outBuffer = new StringWriter();
            out = new PrintWriter(outBuffer);
            state = JobState.initialized;
        }

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                threadPool.execute(thread);
            }
        }

        public void stop() {
            if (thread != null) {
                LOG.info ("stop script execution...");
                thread.interrupt();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException intex) {
                    LOG.warn("wait interrupted (" + intex.getMessage() + ")");
                }
                if (state == JobState.running) {
                    state = JobState.aborted;
                    LOG.warn("can't interrupt thread... stopped!");
                    thread.stop();
                }
                thread = null;
                close();
            }
        }

        public void close() {
            if (session != null) {
                session.logout();
                session = null;
            }
        }

        public void run() {
            state = JobState.starting;
            runner = new GroovyRunner(session, out, setupScript);
            try {
                Node node = session.getNode(scriptPath);
                Binary binary = PropertyUtil.getBinaryData(node);
                if (binary != null) {
                    try {
                        InputStream in = binary.getStream();
                        Reader reader = new InputStreamReader(in, "UTF-8");
                        state = JobState.running;
                        try {
                            runner.run(reader, null);
                            state = JobState.finished;
                        } catch (InterruptedException intex) {
                            LOG.warn(intex.getMessage());
                            state = JobState.aborted;
                        } catch (Exception ex) {
                            LOG.error(ex.getMessage(), ex);
                            state = JobState.error;
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
                    out.write("can't load script: " + scriptPath);
                }
            } catch (RepositoryException rex) {
                state = JobState.error;
                out.write("can't load script: " + rex);
            } finally {
                thread = null;
                close();
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

    public JobState startScript(String key, Session session, String scriptPath, PrintWriter out)
            throws IOException, RepositoryException {
        ScriptJob scriptJob = new ScriptJob(key, session, scriptPath);
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
                    scriptJob.out.write("\nscript execution aborted!");
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

        // try availability of the groovy framework
        GroovyShell shell = new GroovyShell();

        ModifiableThreadPoolConfig threadPoolConfig = new ModifiableThreadPoolConfig();
        threadPoolConfig.setMaxPoolSize(0);
        threadPoolConfig.setMaxPoolSize(5);
        threadPoolConfig.setPriority(ThreadPoolConfig.ThreadPriority.NORM);
        threadPool = threadPoolManager.create(threadPoolConfig);

        runningJobs = new HashMap<>();

        setupScript = (String) coreConfig.getProperties().get(CoreConfiguration.GROOVY_SETUP_SCRIPT);
        if (StringUtils.isBlank(setupScript)) {
            setupScript = GroovyRunner.DEFAULT_SETUP_SCRIPT;
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) throws Exception {
        synchronized (runningJobs) {
            for (Map.Entry<String, ScriptJob> entry : runningJobs.entrySet()) {
                entry.getValue().stop();
            }
            runningJobs.clear();
        }
        if (threadPool != null) {
            threadPoolManager.release(threadPool);
            threadPool = null;
        }
    }
}