package com.composum.sling.core.proxy;

import com.composum.sling.core.util.ValueEmbeddingReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.HttpService;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a configurable proxy request service factory usable directly and also as a base for special proxy implementations
 */
@Component(service = GenericProxyService.class, scope = ServiceScope.PROTOTYPE)
@Designate(ocd = GenericProxyConfig.class, factory = true)
public class GenericProxyRequest implements GenericProxyService {

    private static final Logger LOG = LoggerFactory.getLogger(GenericProxyRequest.class);

    public static final Pattern XML_CONTENT_URL = Pattern.compile("^.*/[^/]+\\.(html|xml)(\\?.*)?$");
    public static final Pattern XML_CONTENT_TYPE = Pattern.compile("^text/(html|xml)(;.*)?$");

    protected GenericProxyConfig config;

    protected Pattern targetPattern;

    protected BundleContext bundleContext;

    @Activate
    @Modified
    protected void activate(final ComponentContext context, final GenericProxyConfig config) {
        this.bundleContext = context.getBundleContext();
        this.config = config;
        if (config.enabled()) {
            String rule = config.targetPattern();
            if (StringUtils.isNotBlank(rule)) {
                targetPattern = Pattern.compile(rule.startsWith("/") ? ("^" + rule) : rule);
            }
        }
    }

    @Override
    @NotNull
    public String getName() {
        return config.name();
    }


    /**
     * Handles the proxy request if appropriate (target pattern matches and access allowed)
     *
     * @param request   the proxy request
     * @param response  the response for the answer
     * @param targetUrl the url of the request which is addressing the target
     * @return 'true' if the request is supported by the service, allowed for the user and handle by the service
     */
    @Override
    public boolean doProxy(@NotNull final SlingHttpServletRequest request,
                           @NotNull final SlingHttpServletResponse response,
                           @NotNull final String targetUrl)
            throws IOException {
        if (config.enabled()) {
            Matcher matcher = targetPattern.matcher(targetUrl);
            if (matcher.find()) {
                try {
                    boolean allowed = false;
                    String referencePath = config.referencePath();
                    if (StringUtils.isNotBlank(referencePath)) {
                        ResourceResolver resolver = request.getResourceResolver();
                        if (referencePath.startsWith("/")) {
                            allowed = resolver.getResource(referencePath) != null;
                        } else {
                            for (String root : resolver.getSearchPath()) {
                                allowed = resolver.getResource(root + referencePath) != null;
                                if (allowed) {
                                    break;
                                }
                            }
                        }
                    }
                    if (allowed) {
                        doRequest(request, response, targetUrl, matcher);
                    } else {
                        return false;
                    }
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                return true; // if this service was the right one signal the proxy handling even if an erros has occured
            }
        }
        return false;
    }

    /**
     * Send the request to the proxies target and sends the reveived answer ans response
     *
     * @param request   the request to the proxy servlet
     * @param response  the response of the rquest to the proxy servlet
     * @param targetRef the URL derived from the request to the proxy servlet
     * @param matcher   the prepared matcher used to determine this proxy service implementation as the right one
     */
    protected void doRequest(@NotNull final SlingHttpServletRequest request,
                             @NotNull final SlingHttpServletResponse response,
                             @NotNull final String targetRef,
                             @NotNull final Matcher matcher)
            throws Exception {
        String targetUrl = getTargetUrl(request, targetRef, matcher);
        if (StringUtils.isNotBlank(targetUrl)) {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(targetUrl);
            httpGet.addHeader("Cookie", request.getHeader("Cookie"));
            LOG.info("proxy request '{}'", httpGet.getRequestLine());
            try (CloseableHttpResponse targetResponse = client.execute(httpGet)) {
                final HttpEntity entity = targetResponse.getEntity();
                if (entity != null) {
                    doResponse(request, response, targetUrl, entity);
                } else {
                    LOG.warn("response is NULL ({})", targetUrl);
                }
            }
        } else {
            LOG.info("no target URL: NOP ({})", targetRef);
        }
    }

    /**
     * Prepare, filter and deliver the content entity reveived from the target.
     *
     * @param request   the request to the proxy servlet
     * @param response  the response object to send the answer
     * @param targetUrl the URL used to request the entity
     * @param entity    the content received from the target
     */
    protected void doResponse(@NotNull final SlingHttpServletRequest request,
                              @NotNull final SlingHttpServletResponse response,
                              @NotNull final String targetUrl,
                              @NotNull final HttpEntity entity)
            throws IOException {
        try (InputStream inputStream = entity.getContent()) {
            String contentType = getContentType(targetUrl, entity);
            if (StringUtils.isNotBlank(contentType)) {
                response.setContentType(contentType);
            }
            if (contentType != null && XML_CONTENT_TYPE.matcher(contentType).matches()) {
                SAXTransformerFactory stf = null;
                XMLFilter xmlFilter = null;
                String[] xsltChainPaths = config.XSLT_chain_paths();
                if (xsltChainPaths.length > 0) {
                    // build XML filter for XSLT transformation
                    stf = (SAXTransformerFactory) TransformerFactory.newInstance();
                    xmlFilter = getXsltFilter(stf, request.getResourceResolver(), xsltChainPaths);
                }
                if (xmlFilter != null) {
                    // do XSLT transformation (probably pre-filtered by the reader)...
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("XSLT transformation ({})...", xmlFilter);
                    }
                    try (Reader entityReader = getContentReader(targetUrl, inputStream)) {
                        Transformer transformer = stf.newTransformer();
                        SAXSource transformSource = new SAXSource(xmlFilter, new InputSource(entityReader));
                        transformer.transform(transformSource, new StreamResult(response.getWriter()));
                    } catch (Exception ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("pumping HTML/XML content...");
                    }
                    // stream entity response (probably filtered by the reader)
                    try (Reader entityReader = getContentReader(targetUrl, inputStream)) {
                        IOUtils.copy(entityReader, response.getWriter());
                    } catch (Exception ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("pumping non XML content...");
                }
                // stream entity response (probably filtered by the reader)
                response.setContentLength((int) entity.getContentLength());
                try (Reader entityReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    IOUtils.copy(entityReader, response.getWriter());
                }
            }
        }
    }

    /**
     * @return the type of the requested content determined from the entity or the requested URL
     */
    @Nullable
    protected String getContentType(@NotNull final String targetUrl, @NotNull final HttpEntity entity) {
        Header type = entity.getContentType();
        if (type != null) {
            Header encoding = entity.getContentEncoding();
            return encoding != null ? type.getValue() + ";charset=" + encoding.getValue() : type.getValue();
        } else {
            Matcher matcher = XML_CONTENT_URL.matcher(targetUrl);
            return matcher.matches() ? "text/" + matcher.group(0) + ";charset=utf-8" : null;
        }
    }

    /**
     * the factory method for the reader to prepare and filter the content received from the target
     *
     * @param targetUrl     the URL used to request the entity
     * @param entityContent the received content as stream
     * @return the reader to use to receive the content
     */
    @NotNull
    protected Reader getContentReader(@NotNull final String targetUrl,
                                      @NotNull final InputStream entityContent) {
        String[] toRename = config.tags_to_rename();
        String[] toStrip = config.tags_to_strip();
        String[] toDrop = config.tags_to_drop();
        Reader reader = toRename.length > 0 || toStrip.length > 0 || toDrop.length > 0
                ? new GenericProxyReader(entityContent, toRename, toStrip, toDrop)
                : new InputStreamReader(entityContent, StandardCharsets.UTF_8);
        if (LOG.isDebugEnabled()) {
            LOG.debug("using reader '{}' ({})", reader, targetUrl);
        }
        return reader;
    }

    /**
     * Builds the URL for the target request using the URI built by the ProxyServlet and the matcher of that URI.
     *
     * @param request   the original request received by the ProxyServlet
     * @param targetRef the target URI derived from the original request (suffix + query string)
     * @param matcher   the URI pattern matcher (gives access to the groups declared by the pattern)
     * @return the URL for the HTTP request to the target
     */
    @Nullable
    protected String getTargetUrl(@NotNull final SlingHttpServletRequest request,
                                  @NotNull final String targetRef, @NotNull final Matcher matcher) {
        String targetUrl = config.targetUrl();
        if (StringUtils.isNotBlank(targetUrl)) {
            // if a targetURL is configured use the configured pattern to build the final URL based on the requested
            // URI; the configured targetUrl can contain value placeholders ${0},${1},... to embed groups of the matcher
            Map<String, Object> properties = new HashMap<>();
            addHttpValues(properties);
            properties.put("ctx", request.getContextPath());
            properties.put("url", targetRef);
            for (int i = 0; i < matcher.groupCount(); i++) { // add all available groups as properties
                properties.put(Integer.toString(i), matcher.group(i));
            }
            ValueEmbeddingReader reader = new ValueEmbeddingReader(new StringReader(targetUrl), properties);
            try {   // replace the palceholders of the target URL and embed the referenced properties...
                targetUrl = IOUtils.toString(reader);
            } catch (IOException ex) {
                LOG.error(ex.toString());
                targetUrl = null;
            }
        } else {
            targetUrl = targetRef.startsWith("/") // complete a path and prepend host and port
                    ? (request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + targetRef)
                    : targetRef;
        }
        return targetUrl;
    }

    protected void addHttpValues(@NotNull final Map<String, Object> values) {
        ServiceReference<HttpService> service = bundleContext.getServiceReference(HttpService.class);
        String endpoint = Arrays.asList((String[]) service.getProperty("osgi.http.endpoint")).get(0);
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        values.put("http.endpoint", endpoint);
        values.put("http.port", Integer.parseInt(service.getProperty("org.osgi.service.http.port").toString()));
    }

    //
    // XSLT transformation
    //

    /**
     * @return a chain of XML filters initialized with the XSLT resources resolved from the given path list
     */
    @Nullable
    protected XMLFilter getXsltFilter(@NotNull final SAXTransformerFactory stf,
                                      @NotNull final ResourceResolver resolver,
                                      @NotNull final String[] xsltChainPaths) {
        XMLFilter xmlFilter = null;
        try {
            for (String xsltPath : xsltChainPaths) {
                XMLFilter next = getXmlFilter(stf, resolver.getResource(xsltPath));
                if (next != null) {
                    if (xmlFilter == null) {
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        spf.setNamespaceAware(true);
                        spf.setValidating(false);
                        SAXParser parser = spf.newSAXParser();
                        XMLReader reader = parser.getXMLReader();
                        next.setParent(reader);
                    } else {
                        next.setParent(xmlFilter);
                    }
                    xmlFilter = next;
                }
            }
        } catch (ParserConfigurationException | SAXException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return xmlFilter;
    }

    public static XMLFilter getXmlFilter(@NotNull final SAXTransformerFactory stf,
                                         @Nullable final Resource xsltResource) {
        XMLFilter filter = null;
        InputStream inputStream = getFileContent(xsltResource);
        if (inputStream != null) {
            try {
                filter = stf.newXMLFilter(new StreamSource(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
            } catch (TransformerConfigurationException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return filter;
    }

    @Nullable
    public static InputStream getFileContent(@Nullable Resource resource) {
        InputStream inputStream = null;
        if ((resource = getFileResource(resource)) != null) {
            ValueMap values = resource.getValueMap();
            inputStream = values.get(JcrConstants.JCR_DATA, InputStream.class);
        }
        return inputStream;
    }

    @Nullable
    public static Resource getFileResource(@Nullable final Resource resource) {
        return resource != null && resource.isResourceType(JcrConstants.NT_FILE)
                ? resource.getChild(JcrConstants.JCR_CONTENT) : resource;
    }
}
