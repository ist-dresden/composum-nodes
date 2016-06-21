package com.composum.sling.nodes.servlet;

import com.composum.sling.core.exception.ParameterValidationException;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.PropertyUtil;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * the strategy and their implementations to create new nodes in various types during the
 * 'create' operation of the 'node' servlet
 */
public class NodeFactory {

    public static final NodeFactory SINGLETON = new NodeFactory();

    protected TypeStrategy defaultStrategy;
    protected Map<String, TypeStrategy> strategyMap;

    public NodeFactory() {
        strategyMap = new HashMap<>();
        strategyMap.put(ResourceUtil.TYPE_OAKINDEX, new OakIndexStrategy());
        strategyMap.put(ResourceUtil.TYPE_FILE, new NtFileStrategy());
        strategyMap.put(ResourceUtil.TYPE_LINKED_FILE, new NtLinkedFileStrategy());
        strategyMap.put(ResourceUtil.TYPE_RESOURCE, new NtResourceStrategy());
        defaultStrategy = new DefaultStrategy();
    }

    public Node createNode(SlingHttpServletRequest request, Node parentNode,
                           String name, NodeServlet.NodeParameters parameters)
            throws RepositoryException, IOException, ParameterValidationException {
        TypeStrategy strategy = strategyMap.get(parameters.type);
        if (strategy == null) {
            strategy = defaultStrategy;
        }
        Node node = strategy.createNode(request, parentNode, name, parameters);
        return node;
    }

    public interface TypeStrategy {

        Node createNode(SlingHttpServletRequest request, Node parentNode,
                        String name, NodeServlet.NodeParameters parameters)
                throws RepositoryException, IOException, ParameterValidationException;
    }

    public static class DefaultStrategy implements TypeStrategy {

        public Node createNode(SlingHttpServletRequest request, Node parentNode,
                               String name, NodeServlet.NodeParameters parameters)
                throws RepositoryException, ParameterValidationException {

            Node node = parentNode.addNode(name, parameters.type);

            if (StringUtils.isNotBlank(parameters.title)) {
                node.setProperty(ResourceUtil.PROP_TITLE, parameters.title);
            }

            if (StringUtils.isNotBlank(parameters.resourceType)) {
                node.setProperty(ResourceUtil.PROP_RESOURCE_TYPE, parameters.resourceType);
            }

            return node;
        }
    }

    public static class OakIndexStrategy implements TypeStrategy {

        public Node createNode(SlingHttpServletRequest request, Node parentNode,
                               String name, NodeServlet.NodeParameters params)
                throws RepositoryException, IOException, ParameterValidationException {

            Node node = parentNode.addNode(name, params.type);

            RequestParameterMap parameters = request.getRequestParameterMap();

            Property property = null;

            RequestParameter idxType = parameters.getValue("indexType");
            if (idxType != null) {
                property = PropertyUtil.setProperty(node, "type", idxType.getString(), PropertyType.STRING);
                property = PropertyUtil.setProperty(node, "propertyNames", new ArrayList<String>(), PropertyType.NAME);
            }

            return node;
        }
    }

    public static class NtFileStrategy implements TypeStrategy {

        public Node createNode(SlingHttpServletRequest request, Node parentNode,
                               String name, NodeServlet.NodeParameters params)
                throws RepositoryException, IOException, ParameterValidationException {

            Node node = parentNode.addNode(name, params.type);

            Node contentNode = node.addNode(ResourceUtil.CONTENT_NODE, ResourceUtil.TYPE_RESOURCE);

            if (StringUtils.isNotBlank(params.mimeType)) {
                contentNode.setProperty(ResourceUtil.PROP_MIME_TYPE, params.mimeType);
            }

            RequestParameterMap parameters = request.getRequestParameterMap();

            Property property = null;
            RequestParameter file = parameters.getValue(AbstractServiceServlet.PARAM_FILE);
            if (file != null) {
                InputStream input = file.getInputStream();
                property = PropertyUtil.setProperty(contentNode, ResourceUtil.PROP_DATA, input);
            }

            // set mime type by the received content if not specified
            if (StringUtils.isBlank(params.mimeType)) {
                setMimTypeByData(name, contentNode, property);
            }

            return node;
        }
    }

    public static class NtLinkedFileStrategy implements TypeStrategy {

        public Node createNode(SlingHttpServletRequest request, Node parentNode,
                               String name, NodeServlet.NodeParameters params)
                throws RepositoryException, IOException, ParameterValidationException {

            Node node = null;
            if (StringUtils.isNotBlank(params.jcrContent)) {

                Session session = parentNode.getSession();

                Node linkTarget = session.getNode(params.jcrContent);
                if (linkTarget != null && linkTarget.isNodeType(ResourceUtil.TYPE_REFERENCEABLE)) {

                    String identifier = linkTarget.getIdentifier();
                    node = parentNode.addNode(name, params.type);
                    node.setProperty(ResourceUtil.PROP_JCR_CONTENT, identifier, PropertyType.REFERENCE);
                }
            }
            if (node == null) {
                throw new ParameterValidationException("invalid link target '" + params.jcrContent + "'");
            }
            return node;
        }
    }

    public static class NtResourceStrategy implements TypeStrategy {

        public Node createNode(SlingHttpServletRequest request, Node parentNode,
                               String name, NodeServlet.NodeParameters params)
                throws RepositoryException, IOException, ParameterValidationException {

            Node node = parentNode.addNode(name, params.type);

            if (StringUtils.isNotBlank(params.mimeType)) {
                node.setProperty(ResourceUtil.PROP_MIME_TYPE, params.mimeType);
            }

            RequestParameterMap parameters = request.getRequestParameterMap();

            Property property = null;
            RequestParameter file = parameters.getValue(AbstractServiceServlet.PARAM_FILE);
            if (file != null) {
                InputStream input = file.getInputStream();
                property = PropertyUtil.setProperty(node, ResourceUtil.PROP_DATA, input);
            }

            // set mime type by the received content if not specified
            if (StringUtils.isBlank(params.mimeType)) {
                setMimTypeByData(name, node, property);
            }

            return node;
        }
    }

    public static void setMimTypeByData(String name, Node node, Property property) throws RepositoryException {
        if (property != null) {
            String mimeType = MimeTypeUtil.getMimeType(name, property, null);
            if (StringUtils.isNotBlank(mimeType)) {
                node.setProperty(ResourceUtil.PROP_MIME_TYPE, mimeType);
            }
        }
    }
}
