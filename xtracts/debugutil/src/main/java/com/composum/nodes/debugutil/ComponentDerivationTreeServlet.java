package com.composum.nodes.debugutil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.jcr.query.Query;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Makes a tree of all derived components (connected with sling:resourceSuperType).
 * Usage: <code>
 * wget --user=admin --password=admin -O componenttree.dotty http://localhost:9090/bin/cpm/nodes/debug/componenttree.dotty ; <br/>
 * ccomps -x componenttree.dotty | dot | gvpack | neato -Tpng -n2 -o componenttree.png ; open componenttree.png </code>
 * There can also be a parameter regex to filter the components we consider.
 * <p>
 * For mermaid diagram (to include e.g. into Github markdown), use extension .mermaid, e.g.
 * http://localhost:4502/bin/cpm/nodes/debug/componenttree.mermaid?regex=wknd
 * </p>
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Debugutil Component Tree Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/nodes/debug/componenttree",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ComponentDerivationTreeServlet.Configuration.class)
public class ComponentDerivationTreeServlet extends SlingSafeMethodsServlet {

    /**
     * Optional regex to limit components' path.
     */
    protected static final String PARAM_REGEX = "regex";

    protected volatile Configuration config;
    protected volatile Pattern ignoreComponentsRegex;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
        if (config == null || !config.enabled()) {
            throw new IllegalStateException("Not enabled.");
        }
        ResourceResolver resolver = request.getResourceResolver();
        response.setContentType("text/plain;charset=UTF-8");
        Pattern regex = Pattern.compile(StringUtils.defaultString(request.getParameter(PARAM_REGEX)));
        Map<String, String> derivations = new TreeMap<>();
        for (Resource resource : IteratorUtils.asIterable(resolver.findResources("/jcr:root/(apps|libs)//*[@sling:resourceSuperType]", Query.XPATH))) {
            String supertype = resource.getValueMap().get("sling:resourceSuperType", String.class);
            String ourtype = StringUtils.removeFirst(resource.getPath(), "^/(libs|apps)/");
            if (supertype != null && !ignoreComponentsRegex.matcher(supertype).matches() && regex.matcher(ourtype).find()) {
                derivations.put(ourtype, supertype);
            }
        }
        try (PrintWriter out = response.getWriter()) {
            if (request.getRequestPathInfo().getExtension().equals("mermaid")) {
                out.println("```mermaid");
                out.println("graph TD");
                Set<String> nodes = new TreeSet<>();
                nodes.addAll(derivations.keySet());
                nodes.addAll(derivations.values());
                for (String node : nodes) {
                    out.println(escape(node) + "[\"" + node + "\"]");
                }
                for (Map.Entry<String, String> entry : derivations.entrySet()) {
                    out.println(escape(entry.getKey()) + " --> " + escape(entry.getValue()));
                }
                out.println("```");
            } else { // default dotty
                out.println("digraph componentree {");
                for (Map.Entry<String, String> entry : derivations.entrySet()) {
                    out.println('"' + entry.getKey() + "\" -> \"" + entry.getValue() + "\" ;");
                }
                out.println("}");
            }
        }
    }

    String escape(String s) {
        // replace all nonalphanumeric characters with _
        return s.replaceAll("[^a-zA-Z0-9]", "_");
    }

    @ObjectClassDefinition(
            name = "Composum Nodes Debugutil Component Derivation Tree Servlet",
            description = "Makes a tree of all derived components (connected with sling:resourceSuperType).\n" +
                    " Usage e.g.: \n" +
                    " wget --user=admin --password=admin -O componenttree.dotty http://localhost:9090/bin/cpm/nodes/debug/componenttree.dotty ; \n" +
                    " ccomps -x componenttree.dotty | dot | gvpack | neato -Tpng -n2 -o componenttree.png ; open componenttree.png \n" +
                    " There can also be a parameter regex to filter the components we consider."
    )
    public @interface Configuration {
        @AttributeDefinition(
                description = "Enable the servlet"
        )
        boolean enabled() default false;

        @AttributeDefinition(
                description = "Regex to ignore supertypes: Some components are just too frequent - we ignore these, or the picture gets too large."
        )
        String ignoredComponentsRegex() default "^(composum/pages/stage/edit/default/element/tile)$";
    }

    @Activate
    @Modified
    protected void activate(Configuration config) {
        this.config = null;
        this.ignoreComponentsRegex =
                Pattern.compile(StringUtils.defaultIfBlank(config.ignoredComponentsRegex(), "shouldnevermatch,dcfxgirtensodsisdfr04ker9sdkl"));
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        this.config = null;
    }

}
