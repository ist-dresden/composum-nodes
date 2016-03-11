package com.composum.sling.core.script;

import com.composum.sling.core.util.ResourceUtil;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.QueryManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rw on 06.10.15.
 */
public class GroovyRunner {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyRunner.class);

    public static final String DEFAULT_SETUP_SCRIPT = "script/setup.groovy";
    public static final String JAVA_RESOURCE_BASE = "/com/composum/sling/core/";
    public static final String ENCODING = "UTF-8";

    protected BundleContext bundleContext;
    protected ResourceResolverFactory resourceResolverFactory;
    protected ResourceResolver resourceResolver;
    protected Session session;
    protected Workspace workspace;
    protected QueryManager queryManager;
    protected PrintWriter out;

    protected Map<String, Object> generalBindings = new HashMap();

    protected String setupScript;

    public GroovyRunner(Session session, PrintWriter out) {
        this(session, out, DEFAULT_SETUP_SCRIPT);
    }

    public GroovyRunner(Session session, PrintWriter out, String setupScript) {
        this.out = out;
        this.session = session;
        this.setupScript = setupScript;
        if (session != null) {
            bundleContext = getBundleContext();
            resourceResolverFactory = getResourceResolverFactory();
            resourceResolver = getResolver();
            workspace = session.getWorkspace();
            queryManager = getQueryManager(workspace);
        }
        generalBindings.put("out", out);
        generalBindings.put("log", LOG);
        generalBindings.put("bundleContext", bundleContext);
        generalBindings.put("resourceResolverFactory", resourceResolverFactory);
        generalBindings.put("resourceResolver", resourceResolver);
        generalBindings.put("session", session);
        generalBindings.put("workspace", workspace);
        generalBindings.put("queryManager", queryManager);
    }

    public Object run(String path, Map<String, Object> variables) throws InterruptedException {
        Object result = null;
        Reader reader = getScriptResource(path);
        if (reader != null) {
            try {
                try {
                    result = run(reader, variables);
                } finally {
                    reader.close();
                }
            } catch (IOException ioex) {
                LOG.error(ioex.getMessage(), ioex);
            }
        }
        return result;
    }

    public Object run(Reader scriptReader, Map<String, Object> variables) throws InterruptedException {
        Script script = getScript(scriptReader, variables);
        Object setupVariables = setup(script);
        extendBinding(script, setupVariables);
        extendBinding(script, generalBindings);
        Object result = script.run();
        return result;
    }

    protected Script getScript(Reader scriptReader, Map<String, Object> variables) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        Binding binding = new Binding(variables);
        GroovyShell shell = new GroovyShell(binding);
        Script script = shell.parse(scriptReader);
        return script;
    }

    protected void extendBinding(Script script, Object variables) {
        extendBinding(script.getBinding(), variables);
    }

    protected void extendBinding(Binding binding, Object variables) {
        if (variables instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) variables).entrySet()) {
                String name = entry.getKey();
                try {
                    if (binding.getVariable(name) == null) {
                        binding.setVariable(name, entry.getValue());
                    }
                } catch (MissingPropertyException mpex){
                    binding.setVariable(name, entry.getValue());
                }
            }
        }
    }

    protected Object setup(Script script) {
        Object result = null;
        Reader reader = getScriptResource(setupScript);
        if (reader != null) {
            try {
                try {
                    Map<String, Object> variables = new HashMap();
                    variables.put("script", script);
                    variables.put("log", LOG);
                    variables.put("out", out);
                    Script setupScript = getScript(reader, variables);
                    extendBinding(setupScript, generalBindings);
                    result = setupScript.run();
                } finally {
                    reader.close();
                }
            } catch (IOException ioex) {
                LOG.error(ioex.getMessage(), ioex);
            }
        }
        return result;
    }

    protected Reader getScriptResource(String path) {
        Reader reader = null;
        if (resourceResolver != null) {
            Resource scriptResource = resourceResolver.getResource(path);
            if (scriptResource != null) {
                Binary binary = ResourceUtil.getBinaryData(scriptResource);
                if (binary != null) {
                    try {
                        InputStream inputStream = binary.getStream();
                        if (inputStream != null) {
                            reader = new InputStreamReader(inputStream, ENCODING);
                        }
                    } catch (UnsupportedEncodingException ueex) {
                        LOG.error(ueex.getMessage(), ueex);
                    } catch (RepositoryException rex) {
                        LOG.error(rex.getMessage(), rex);
                    }
                }
            }
        }
        if (reader == null) {
            InputStream stream = getClass().getResourceAsStream(JAVA_RESOURCE_BASE + path);
            if (stream != null) {
                try {
                    reader = new InputStreamReader(stream, ENCODING);
                } catch (UnsupportedEncodingException ueex) {
                    LOG.error(ueex.getMessage(), ueex);
                }
            }
        }
        return reader;
    }

    protected BundleContext getBundleContext() {
        Bundle resourceResolverBundle = FrameworkUtil.getBundle(ResourceResolverFactory.class);
        BundleContext bundleContext = resourceResolverBundle.getBundleContext();
        return bundleContext;
    }

    protected ResourceResolverFactory getResourceResolverFactory() {
        ServiceReference resourceResolverFactoryReference = bundleContext
                .getServiceReference(ResourceResolverFactory.class);
        ResourceResolverFactory resourceResolverFactory = (ResourceResolverFactory) bundleContext
                .getService(resourceResolverFactoryReference);
        return resourceResolverFactory;
    }

    protected ResourceResolver getResolver() {

        ResourceResolver resolver = null;

        HashMap<String, Object> authMap = new HashMap();
        authMap.put("user.jcr.session", session);

        try {
            resolver = resourceResolverFactory.getResourceResolver(authMap);
        } catch (LoginException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return resolver;
    }

    protected QueryManager getQueryManager(Workspace workspace) {
        QueryManager queryManager = null;
        try {
            queryManager = workspace.getQueryManager();
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return queryManager;
    }
}
