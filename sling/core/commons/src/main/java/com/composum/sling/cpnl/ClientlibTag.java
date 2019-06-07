package com.composum.sling.cpnl;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.handle.ClientlibElement;
import com.composum.sling.clientlibs.handle.ClientlibRef;
import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.clientlibs.service.ClientlibService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * A tag build references to styles and script files. Renders either links to individual files, client libraries
 * containing several embedded files or client library categories containing embedded files for a category of client
 * libraries.
 */
public class ClientlibTag extends CpnlBodyTagSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibTag.class);

    public static final String ALREADY_EMBEDDED = "clientlib.alreadyEmbedded";

    protected Clientlib.Type type;
    protected String path;
    protected String category;
    protected Object test;
    private transient Boolean testResult;

    public void setType(String type) {
        this.type = Clientlib.Type.valueOf(type);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * the 'test' expression for conditional tags
     */
    public void setTest(Object value) {
        test = value;
    }

    /**
     * evaluates the test expression if present and returns the evaluation result; default: 'true'
     */
    protected boolean getTestResult() {
        if (testResult == null) {
            testResult = eval(test, test instanceof Boolean ? (Boolean) test : Boolean.TRUE);
        }
        return testResult;
    }

    @Override
    protected void clear() {
        super.clear();
        type = null;
        path = null;
        category = null;
        test = null;
        testResult = null;
    }

    protected Clientlib.Type getType() {
        if (type == null) {
            String ext = StringUtils.substringAfterLast(path, ".").toLowerCase();
            try {
                ext = ext.replaceAll("(png|jpg)", "img");
                type = Clientlib.Type.valueOf(ext);
            } catch (Exception ex) {
                type = Clientlib.Type.link;
            }
        }
        return type;
    }

    @Override
    public int doEndTag() throws JspException {
        if (getTestResult()) {
            try {
                RendererContext rendererContext = RendererContext.instance(context, request);

                Clientlib.Type type = getType();
                ClientlibRef ref = null;

                if (StringUtils.isNotBlank(path)) {
                    ref = new ClientlibRef(type, path, false, null);
                    LOG.debug("<cpn:clientlib.{} path={}/>", type, path);
                } else if (StringUtils.isNotBlank(category)) {
                    ref = ClientlibRef.forCategory(type, category, false, null);
                    LOG.debug("<cpn:clientlib.{} category={}/>", type, category);
                } else {
                    LOG.error("No path nor category attribute was given!");
                }

                ClientlibService service = context.getService(ClientlibService.class);
                ClientlibElement clientlib = service.resolve(ref, request.getResourceResolver());

                if (null != clientlib) { // if this is a clientlib or category
                    JspWriter writer = this.pageContext.getOut();
                    if (service.getClientlibConfig().getTagDebug()) {
                        writer.println("<!-- cpn:clientlib." + type + " " + defaultIfNull(path, "") +
                                " " + defaultIfNull(category, "") + " -->");
                    }
                    service.renderClientlibLinks(clientlib, writer, request, rendererContext);
                } else {
                    LOG.error("No clientlib found for path '{}' / category '{}' ", path, category);
                }
            } catch (IOException | RepositoryException e) {
                LOG.error(e.getMessage(), e);
            }
            super.doEndTag();
        }
        return EVAL_PAGE;
    }
}
