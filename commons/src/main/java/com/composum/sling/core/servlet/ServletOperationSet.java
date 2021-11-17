package com.composum.sling.core.servlet;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.mapping.MappingRules;
import com.composum.sling.core.util.RequestUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of operation for the implementation of one servlet based on different operations.
 * This set manages the operation for the servlet and is a delegate for the servlet interface methods.
 */
public class ServletOperationSet<E extends Enum<?>, O extends Enum<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(ServletOperationSet.class);

    public enum Method {GET, POST, PUT, DELETE}

    /** the default operation keys for each provided extension */
    protected Map<Method, Map<E, O>> operationDefaults = new HashMap<>();

    /** the operations map set up during servlet initialization */
    protected Map<Method, Map<E, Map<O, ServletOperation>>> operationMap = new HashMap<>();

    protected final E defaultExtension;

    public ServletOperationSet(E defaultExtension) {
        this.defaultExtension = defaultExtension;
    }

    /**
     * Retrieves the servlet operation requested for the used HTTP method.
     * Looks in the selectors for a operation and gives their implementation in the extensions context.
     *
     * @param request the servlet request
     * @param method  the requested HTTP method
     * @return the operation or 'null', if the requested combination of selector
     * and extension has no implementation for the given HTTP method
     */
    public ServletOperation getOperation(SlingHttpServletRequest request, Method method) {
        ServletOperation operation = null;
        E extension = RequestUtil.getExtension(request, defaultExtension);
        Map<E, O> extensionDefaults = operationDefaults.get(method);
        if (extensionDefaults != null) {
            O defaultOperation = extensionDefaults.get(extension);
            if (defaultOperation != null) {
                Map<E, Map<O, ServletOperation>> extensions = operationMap.get(method);
                if (extensions != null) {
                    Map<O, ServletOperation> operations = extensions.get(extension);
                    if (operations != null) {
                        operation = operations.get(RequestUtil.getSelector(request, defaultOperation));
                    }
                }
            }
        }
        return operation;
    }

    public void setOperation(Method method, E extension, O operation, ServletOperation implementation) {
        setOperation(method, extension, operation, implementation, false);
    }

    public void setOperation(Method method, E extension, O operation,
                             ServletOperation implementation, boolean isDefault) {
        Map<E, Map<O, ServletOperation>> extensions = operationMap.computeIfAbsent(method, k -> new HashMap<>());
        Map<O, ServletOperation> operations = extensions.computeIfAbsent(extension, k -> new HashMap<>());
        if (implementation != null) {
            operations.put(operation, implementation);
        } else {
            operations.remove(operation);
        }
        // set as default operation if determined as default or if no default operation
        // determined before (the first registered operation is implicit the default)
        if (isDefault || getDefaultOperation(method, extension) == null) {
            setDefaultOperation(method, extension, operation);
        }
    }

    public O getDefaultOperation(Method method, E extension) {
        O operation = null;
        Map<E, O> extensions = operationDefaults.get(method);
        if (extensions != null) {
            operation = extensions.get(extension);
        }
        return operation;
    }

    public void setDefaultOperation(Method method, E extension, O operation) {
        Map<E, O> extensions = operationDefaults.computeIfAbsent(method, k -> new HashMap<>());
        if (operation != null) {
            extensions.put(extension, operation);
        } else {
            extensions.remove(extension);
        }
    }

    //
    // the Servlet interface delegates
    //

    /**
     * the extension hook if the resource is not simply build by the suffix
     */
    protected ResourceHandle getResource(SlingHttpServletRequest request) {
        return AbstractServiceServlet.getResource(request);
    }

    // GET access

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ServletOperation operation = getOperation(request, Method.GET);

        if (operation != null) {
            try {

                ResourceHandle resource = getResource(request);
                operation.doIt(request, response, resource);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                if (response.isCommitted()) {
                    OutputStream log = response.getOutputStream();
                    log.write(ex.toString().getBytes(MappingRules.CHARSET));
                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.toString());
                }
            }

        } else {
            sendInvalidOperation(request, response, Method.GET);
        }
    }

    // POST method

    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ServletOperation operation = getOperation(request, Method.POST);

        if (operation != null) {
            try {

                ResourceHandle resource = getResource(request);
                operation.doIt(request, response, resource);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                if (response.isCommitted()) {
                    OutputStream log = response.getOutputStream();
                    log.write(ex.toString().getBytes(MappingRules.CHARSET));
                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.toString());
                }
            }

        } else {
            sendInvalidOperation(request, response, Method.POST);
        }
    }

    // PUT upload

    protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ServletOperation operation = getOperation(request, Method.PUT);

        if (operation != null) {
            try {

                ResourceHandle resource = getResource(request);
                operation.doIt(request, response, resource);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                if (response.isCommitted()) {
                    OutputStream log = response.getOutputStream();
                    log.write(ex.toString().getBytes(MappingRules.CHARSET));
                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.toString());
                }
            }

        } else {
            sendInvalidOperation(request, response, Method.PUT);
        }
    }

    // DELETE removal

    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ServletOperation operation = getOperation(request, Method.DELETE);

        if (operation != null) {
            try {

                ResourceHandle resource = getResource(request);
                operation.doIt(request, response, resource);

            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
                if (response.isCommitted()) {
                    OutputStream log = response.getOutputStream();
                    log.write(ex.toString().getBytes(MappingRules.CHARSET));
                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.toString());
                }
            }

        } else {
            sendInvalidOperation(request, response, Method.DELETE);
        }
    }

    //
    // helpers
    //

    public void sendInvalidOperation(SlingHttpServletRequest request,
                                     SlingHttpServletResponse response,
                                     Method method) throws IOException {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        String message = "invalid operation '" + method.name() + ":"
                + pathInfo.getSelectorString() + "." + pathInfo.getExtension() + "'";
        try {
            throw new Exception(message);
        } catch (Exception ex) {
            LOG.error(message, ex);
        }
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
    }
}
