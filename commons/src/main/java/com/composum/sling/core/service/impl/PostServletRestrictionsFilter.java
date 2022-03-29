package com.composum.sling.core.service.impl;

import com.composum.sling.core.service.ServiceRestrictions;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;

/**
 * Service restrictions support for POST requests to restict Sling POST Servlet requests.
 */
@Component(
        service = {Filter.class},
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Service Restrictions POST Filter",
                "sling.filter.scope=REQUEST"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
@Designate(ocd = PostServletRestrictionsFilter.Config.class)
public class PostServletRestrictionsFilter implements Filter {

    @ObjectClassDefinition(
            name = "Composum Service Restrictions POST Filter Configuration"
    )
    @interface Config {

        @AttributeDefinition(
                name = "Enabled",
                description = "the on/off switch for the Restrictions Filter (default: true)"
        )
        boolean enabled() default true;

        @AttributeDefinition(
                name = "Path Restrictions",
                description = "the mapping of repository path patterns to service keys (e.g. '^/content(/.*)?$=pages/content/edit')"
        )
        String[] restictedPaths() default {};

        @AttributeDefinition(
                name = "Service Ranking",
                description = "the ranking of the service to place the servlet filter at the right place in the filter chain"
        )
        int service_ranking() default 6500;
    }

    @Reference
    private ServiceRestrictions restrictions;

    private Config config;

    private final Map<Pattern, ServiceRestrictions.Key> restrictedPaths = new LinkedHashMap<>();

    @Activate
    @Modified
    public final void activate(final Config config) {
        this.config = config;
        this.restrictedPaths.clear();
        for (final String rule : config.restictedPaths()) {
            final String[] keyVal = StringUtils.split(rule, "=", 2);
            if (keyVal.length == 2 && StringUtils.isNotBlank(keyVal[0]) && StringUtils.isNotBlank(keyVal[1])) {
                restrictedPaths.put(Pattern.compile(keyVal[0]), new ServiceRestrictions.Key(keyVal[1]));
            }
        }
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain chain)
            throws IOException, ServletException {
        if (config.enabled() && this.restrictedPaths.size() > 0 && servletRequest instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
            if (METHOD_POST.equals(request.getMethod())) {
                final String path = request.getResource().getPath();
                for (Map.Entry<Pattern, ServiceRestrictions.Key> entry : this.restrictedPaths.entrySet()) {
                    if (entry.getKey().matcher(path).matches()
                            && !restrictions.isPermissible(request, entry.getValue(), ServiceRestrictions.Permission.write)) {
                        final SlingHttpServletResponse response = (SlingHttpServletResponse) servletResponse;
                        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                }
            }
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
