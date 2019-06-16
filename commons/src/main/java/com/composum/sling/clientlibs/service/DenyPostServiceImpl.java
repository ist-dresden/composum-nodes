package com.composum.sling.clientlibs.service;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * A {@link SlingPostProcessor} whose purpose is to block the processing of POST requests by the default
 * {@link org.apache.sling.servlets.post.impl.SlingPostServlet} for a configurable set of paths.
 * This prevents accidentially creating JCR nodes when trying to access a servlet that is not active for some reason.
 */
@Component(
        label = "Composum PostServlet Blocker",
        description = "a service that blocks the default SlingPostServlet for configurable paths",
        metatype = true
)
@Service(SlingPostProcessor.class)
public class DenyPostServiceImpl implements SlingPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DenyPostServiceImpl.class);

    public static final String DENIED_PATHS = "deniedpaths";
    @Property(
            name = DENIED_PATHS,
            label = "Denied Paths",
            description = "Regular expressions the request path is compared to. If one of these matches the whole path, the operations called on the SlingPostServlet are rolled back.",
            value = {"/bin/.*"}
    )
    protected String[] deniedpaths;

    @Nonnull
    protected volatile List<Pattern> deniedPathList = Collections.emptyList();

    /**
     * We check whether the resource path matches a configurable set of paths (configured via regex). If it matches, this request is forbidden, and
     * we throw an exception.
     */
    @Override
    public void process(SlingHttpServletRequest request, List<Modification> changes) throws ResourceNotFoundException {
        if (!deniedPathList.isEmpty()) {
            Resource resource = request.getResource();
            String path = resource != null ? resource.getPath() : null;
            if (path != null) {
                for (Pattern pat : deniedPathList) {
                    if (pat.matcher(path).matches()) {
                        LOG.warn("POST to {} reached default SlingPostServlet, but is forbidden via pattern {}", path, pat.pattern());
                        throw new IllegalArgumentException("POSTing to resource via default SlingPostServlet forbidden at this JCR path");
                    }
                }
            }
        }
    }

    @Modified
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        deniedpaths = PropertiesUtil.toStringArray(properties.get(DENIED_PATHS));
        List<Pattern> newpaths = new ArrayList<>();
        if (deniedpaths != null) {
            for (String patternString : deniedpaths) {
                try {
                    Pattern pattern = Pattern.compile(patternString);
                    newpaths.add(pattern);
                } catch (PatternSyntaxException pe) {
                    LOG.error("Pattern not parseable: {}", patternString, pe);
                }
            }
        }
        deniedPathList = newpaths;
    }
}
