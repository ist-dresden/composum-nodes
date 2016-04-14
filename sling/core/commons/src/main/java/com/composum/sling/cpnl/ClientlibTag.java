package com.composum.sling.cpnl;

import com.composum.sling.clientlibs.handle.Clientlib;
import com.composum.sling.clientlibs.processor.RendererContext;
import com.composum.sling.clientlibs.service.ClientlibService;
import com.composum.sling.core.BeanContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * a tag build references to styles and script files
 */
public class ClientlibTag extends CpnlBodyTagSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibTag.class);

    public static final String ALREADY_EMBEDDED = "clientlib.alreadyEmbedded";

    protected Clientlib.Type type;
    protected String path;
    protected Map<String, String> properties = new LinkedHashMap<>();

    public void setType(String type) {
        this.type = Clientlib.Type.valueOf(type);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setRel(String rel) {
        properties.put(Clientlib.PROP_REL, rel);
    }

    @Override
    protected void clear() {
        super.clear();
        type = null;
        path = null;
        properties.clear();
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
        try {
            RendererContext rendererContext = RendererContext.instance(new BeanContext.Page(pageContext), request);

            Clientlib.Type type = getType();
            Clientlib clientlib = new Clientlib(request, path, type);

            if (clientlib.isValid()) {

                JspWriter writer = this.pageContext.getOut();
                ClientlibService service = this.sling.getScriptHelper().getService(ClientlibService.class);
                service.renderClientlibLinks(clientlib, properties, writer, rendererContext);

            } else {
                String path = clientlib.getPath(this.path);
                if (StringUtils.isNotBlank(path)) {
                    if (rendererContext.tryAndRegister(path)) {
                        JspWriter writer = this.pageContext.getOut();
                        switch (type) {
                            case link:
                            case css:
                                String rel = properties.get(Clientlib.PROP_REL);
                                writer.write("<link rel=\"");
                                writer.write(StringUtils.isNotBlank(rel) ? rel : "stylesheet");
                                writer.write("\" href=\"");
                                writer.write(rendererContext.mapClientlibURLs()
                                        ? CpnlElFunctions.url(request, path)
                                        : request.getContextPath() + path);
                                writer.write("\" />");
                                break;
                            case js:
                                writer.write("<script type=\"text/javascript\" src=\"");
                                writer.write(rendererContext.mapClientlibURLs()
                                        ? CpnlElFunctions.url(request, path)
                                        : request.getContextPath() + path);
                                writer.write("\"></script>");
                                break;
                            case img:
                                writer.write("<img src=\"");
                                writer.write(rendererContext.mapClientlibURLs()
                                        ? CpnlElFunctions.url(request, path)
                                        : request.getContextPath() + path);
                                writer.write("\"/>");
                                break;
                            default:
                                break;
                        }
                    } else {
                        LOG.warn("Clientlib (file) '" + path + "' already embedded - igenored here.");
                    }
                } else {
                    LOG.warn("Clientlib (file) '" + this.path + "' not found or not accessible!");
                }
            }
        } catch (IOException ioex) {
            LOG.error(ioex.getMessage(), ioex);
        }
        super.doEndTag();
        return EVAL_PAGE;
    }
}
