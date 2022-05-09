package com.composum.nodes.debugutil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * As an alternative to {@link com.composum.pages.commons.taglib.IncludeTag#debugComment(boolean)} that works on sling:include, too: writes out a HTML comment with the rendered resource and the resource type (since that could be possibly overridden).
 */
@Component(
        service = Filter.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil Renderinfo Comment Logging Filter",
                "sling.filter.scope=REQUEST",
                "sling.filter.scope=INCLUDE",
                "sling.filter.scope=FORWARD",
                "service.ranking:Integer=" + 99999
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = RenderInfoLoggingFilter.Config.class)
public class RenderInfoLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RenderInfoLoggingFilter.class);

    private volatile Pattern urlpattern;
    private volatile Pattern extpattern;

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain) throws IOException, ServletException {
        if (urlpattern == null || extpattern == null ||
                !(rawRequest instanceof SlingHttpServletRequest) || !(rawResponse instanceof SlingHttpServletResponse)) {
            chain.doFilter(rawRequest, rawResponse);
            return;
        }

        SlingHttpServletRequest request = (SlingHttpServletRequest) rawRequest;
        SlingHttpServletResponse response = (SlingHttpServletResponse) rawResponse;
        String uri = request.getRequestURI();
        if (!urlpattern.matcher(uri).matches()) {
            LOG.trace("Not matched: {}", uri);
            chain.doFilter(rawRequest, rawResponse);
            return;
        }
        LOG.trace("Matched: {}", uri);

        RequestPathInfo pathInfo = request.getRequestPathInfo();
        if (!extpattern.matcher("" + pathInfo.getExtension()).matches()) {
            LOG.trace("Extension not matched: {}", pathInfo.getExtension());
            chain.doFilter(rawRequest, rawResponse);
            return;
        }
        LOG.trace("Matched: {}", pathInfo.getExtension());

        Resource resource = request.getResource();
        StringBuilder msgBuf = new StringBuilder(resource.getResourceType());
        if (pathInfo.getSelectorString() != null) {
            msgBuf.append(" @ ").append(pathInfo.getSelectorString());
        }
        msgBuf.append(" : ");
        if (ResourceUtil.isNonExistingResource(resource)) {
            msgBuf.append(" (nex) ");
        } else if (ResourceUtil.isSyntheticResource(resource)) {
            msgBuf.append(" (synth) ");
        }
        msgBuf.append(resource.getPath());
        String msg = msgBuf.toString();
        PrintWriter[] writerCapture = new PrintWriter[1];
        boolean[] closeWritten = new boolean[1];
        closeWritten[0] = false;

        SlingHttpServletResponseWrapper wrappedResponse = new SlingHttpServletResponseWrapper(response) {

            @Override
            public PrintWriter getWriter() throws IOException {
                if (writerCapture[0] == null) {
                    writerCapture[0] = new PrintWriter(getResponse().getWriter()) {
                        @Override
                        public void close() {
                            LOG.debug("RenderInfo End: {}", msg);
                            write("<!-- END RENDERINFO: " + msg + " -->\n");
                            closeWritten[0] = true;
                            super.close();
                        }
                    };
                    LOG.debug("RenderInfo Start: {}", msg);
                    writerCapture[0].write("<!-- START RENDERINFO: " + msg + " -->\n");
                }
                return writerCapture[0];
            }
        };

        chain.doFilter(request, wrappedResponse);

        if (!closeWritten[0]) {
            LOG.debug("RenderInfo End: {}", msg);
            writerCapture[0].write("<!-- END RENDERINFO: " + msg + " -->");
            writerCapture[0].flush();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // empty
    }

    @Override
    public void destroy() {
        // empty
    }

    @Activate
    @Modified
    public void activate(final Config config) {
        urlpattern = null;
        if (!"".equals(config.regex().trim()) && !"".equalsIgnoreCase(config.extregex().trim())) {
            urlpattern = Pattern.compile(config.regex());
            extpattern = Pattern.compile(config.extregex());
            LOG.info("activated with {}", config.regex());
        } else {
            LOG.info("deactivated");
        }
    }

    @Deactivate
    public void deactivate() {
        urlpattern = null;
    }

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil Renderinfo Comment Logging Filter",
            description = "Writes out a HTML comments with the rendered resource and the resource type (since that could be possibly overridden) - for each resource included in the page."
    )
    @interface Config {
        @AttributeDefinition(
                name = "URL regex",
                description = "Regular expression that has to match the request's URL. If empty, this filter is inactive."
        )
        String regex();

        @AttributeDefinition(
                name = "Extension regex",
                description = "Regular expression that has to match the request's extension (as parsed by Sling). If empty, this filter is inactive."
        )
        String extregex() default "htm.*";
    }

}
