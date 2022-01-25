package com.composum.nodes.debugutil;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
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
 * Debugging filter: if configured, logs request and response of (preferably non-binary ;-) requests.
 * Useful e.g. to get some examples for documentation.
 */
@Component(
        service = Filter.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil Request Logging Filter",
                "sling.filter.scope=REQUEST",
                "service.ranking:Integer=" + 100000
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = RequestLoggingFilter.Config.class)
public class RequestLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    protected Config config;
    protected Pattern urlpattern;

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain) throws IOException, ServletException {
        if (urlpattern == null || config == null || StringUtils.isBlank(config.regex())
                || !(rawRequest instanceof SlingHttpServletRequest) || !(rawResponse instanceof SlingHttpServletResponse)) {
            chain.doFilter(rawRequest, rawResponse);
            return;
        }

        SlingHttpServletRequest request = (SlingHttpServletRequest) rawRequest;
        SlingHttpServletResponse response = (SlingHttpServletResponse) rawResponse;
        String uri = request.getRequestURI();
        if (!urlpattern.matcher(uri).matches()) {
            chain.doFilter(rawRequest, rawResponse);
            return;
        }

        List<Closeable> closeables = new ArrayList<>();
        try {
            logRequestParameters(request);

            SlingHttpServletRequestWrapper wrappedRequest = new SlingHttpServletRequestWrapper(request) {
                @Override
                public ServletInputStream getInputStream() throws IOException {
                    LOG.info("Inputstream used directly - not logged: {}", uri);
                    return super.getInputStream();
                }

                @Override
                public BufferedReader getReader() throws IOException {
                    BufferedReader reader = new BufferedReader(new LoggingReader(super.getReader(), uri));
                    closeables.add(reader);
                    return reader;
                }
            };
            SlingHttpServletResponseWrapper wrappedResponse = new SlingHttpServletResponseWrapper(response) {
                @Override
                public PrintWriter getWriter() throws IOException {
                    PrintWriter writer = new PrintWriter(new LoggingWriter(super.getWriter(), uri));
                    closeables.add(writer);
                    return writer;
                }

                @Override
                public ServletOutputStream getOutputStream() throws IOException {
                    LOG.info("Outputstream used directly - not logged: {}", uri);
                    return super.getOutputStream();
                }
            };
            chain.doFilter(
                    config.requestContent() ? wrappedRequest : request,
                    config.responseContent() ? wrappedResponse : response);
        } catch (RuntimeException | IOException | ServletException e) {
            LOG.error(uri, e);
            throw e;
        } finally {
            closeables.forEach(IOUtils::closeQuietly);
        }
    }

    protected void logRequestParameters(SlingHttpServletRequest request) {
        StringBuilder buf = new StringBuilder("URL= ").append(request.getRequestURL());
        buf.append("\nuri=").append(request.getRequestURI());
        buf.append("\ninfo=").append(request.getRequestPathInfo());
        Resource resource = request.getResource();
        buf.append("\nresource=").append(resource.getPath());
        if (request.getRequestPathInfo().getSuffixResource() != null) {
            buf.append("\nsuffixResource=").append(request.getRequestPathInfo().getSuffixResource());
        }
        for (Map.Entry<String, RequestParameter[]> entry : request.getRequestParameterMap().entrySet()) {
            for (RequestParameter requestParameter : entry.getValue()) {
                buf.append("\n    ").append(entry.getKey()).append(" = ").append(requestParameter.getString());
            }
        }
        String nex = ResourceUtil.isNonExistingResource(resource) ? "NE" : "EX";
        buf.append("\nRMAP: ").append(request.getRequestURI()).append(" => (" + nex + ") ").append(resource.getPath());
        LOG.info("{}", buf);
    }

    // scaffolding stuff

    @Activate
    @Modified
    public void activate(final Config config) {
        this.config = config;
        if (StringUtils.isNotBlank(config.regex())) {
            urlpattern = null;
            urlpattern = Pattern.compile(config.regex());
        } else {
            urlpattern = null;
        }
    }

    @Deactivate
    public void deactivate() {
        config = null;
        urlpattern = null;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // empty
    }

    @Override
    public void destroy() {
        // empty
    }

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil Request Logging Filter",
            description = "Debugging filter: if configured, logs request and response of (preferably non-binary) requests.\n" +
                    " Useful e.g. to get some examples for documentation."
    )
    @interface Config {

        @AttributeDefinition(
                name = "URL regex",
                description = "Regular expression that has to match the request's URL. If empty, this filter is inactive."
        )
        String regex();

        @AttributeDefinition(
                name = "Log request content"
        )
        boolean requestContent() default false;

        @AttributeDefinition(
                name = "Log response content"
        )
        boolean responseContent() default false;

    }

    protected static class LoggingReader extends FilterReader {
        private final String uri;
        private final StringBuilder buf = new StringBuilder();

        public LoggingReader(Reader reader, String uri) {
            super(reader);
            this.uri = uri;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int numberCharactersRead = super.read(cbuf, off, len);
            if (numberCharactersRead > 0) {
                buf.append(cbuf, off, numberCharactersRead);
            }
            return numberCharactersRead;
        }

        @Override
        public int read() throws IOException {
            int result = in.read();
            if (result >= 0) {
                buf.append((char) result);
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            if (buf.length() > 0) {
                LOG.info("Request for {}:\n{}\n\n", uri, buf.toString());
            }
            buf.setLength(0);
            super.close();
        }
    }

    protected static class LoggingWriter extends FilterWriter {
        protected final String uri;
        protected final StringBuilder buf = new StringBuilder();

        public LoggingWriter(Writer writer, String uri) {
            super(writer);
            this.uri = uri;
        }

        @Override
        public void close() throws IOException {
            if (buf.length() > 0) {
                LOG.info("Response for {}:\n{}\n\n", uri, buf.toString());
            }
            buf.setLength(0);
            super.close();
        }

        @Override
        public void write(int c) throws IOException {
            buf.append((char) c);
            out.write(c);
        }

        @Override
        public void write(char cbuf[], int off, int len) throws IOException {
            buf.append(cbuf, off, len);
            out.write(cbuf, off, len);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            buf.append(str, off, off + len);
            out.write(str, off, len);
        }
    }
}
