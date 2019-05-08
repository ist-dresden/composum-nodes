package com.composum.sling.core.util;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 *
 */
public class NodeUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NodeUtil.class);

    public static final String JCR_TITLE = "jcr:title";

    public static final String FORBIDDEN_NAME_CHARS = "/";

    /**
     * Retrieves the nodes id.
     *
     * @return a hopefully useful ID (not <code>null</code>)
     */
    public String getId(Node node) {
        String id = null;
        try {
            id = node.getIdentifier();
        } catch (RepositoryException e) {
            id = node.toString(); // use Java 'ID'
        }
        return id;
    }

    /**
     * Retrieves the title with a fallback to the nodes name.
     *
     * @param node
     * @return the usable title (not blank)
     */
    public static String getTitle(Node node) {
        String title = getNodeTitle(node);
        if (StringUtils.isBlank(title)) {
            try {
                title = node.getName();
            } catch (RepositoryException rex) {
                LOG.error(rex.getMessage(), rex);
            }
        }
        return title;
    }

    /**
     * Retrieves the 'jcr:title' property value from a node
     *
     * @param node
     * @return the title value or <code>null</code> if not present
     */
    public static String getNodeTitle(Node node) {
        String result = null;
        try {
            Property title = node.getProperty(JCR_TITLE);
            if (title != null) {
                result = title.getString();
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.getMessage(), rex);
        }
        return result;
    }

    /**
     * TODO(rw,2015-04-22) not useful in the core layer
     *
     * @param name
     * @return
     */
    public static String mangleNodeName(String name) {
        if (name != null && name.length() > 0) {
            StringBuilder builder = new StringBuilder();
            int length = name.length();
            char c = name.charAt(0);
            if (c >= '0' && c <= '9') {
                builder.append('_'); // don't start with a digit
            }
            for (int i = 0; i < length; i++) {
                c = name.charAt(i);
                if (c > ' ' && FORBIDDEN_NAME_CHARS.indexOf(c) < 0) {
                    builder.append(c);
                } else {
                    builder.append('_');
                }
            }
            name = builder.toString();
            name = StringEscapeUtils.escapeEcmaScript(name); // prevent from scripting in names
        }
        return name;
    }

    public static boolean isNodeType (Node node, String... nodeType) throws RepositoryException {
        for (String type : nodeType) {
            if (node.isNodeType(type)) {
                return true;
            }
        }
        return false;
    }
}
