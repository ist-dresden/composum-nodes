package com.composum.nodes.debugutil;

import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;
import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_PRIMARYTYPE;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * This servlet serves as code generator to reproduce a part of the JCR tree easily in a unittest using SlingContext.
 * Usage in the browser with e.g. http://localhost:6502/bin/cpm/nodes/debug/jcrtotestsetup.html/{path}
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes JCR to Test Setup Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/nodes/debug/jcrtotestsetup",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = JcrTestSetupGenerateServlet.Configuration.class)
public class JcrTestSetupGenerateServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String path = request.getRequestPathInfo().getSuffix();
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        writer.println("import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_PRIMARYTYPE;\n" +
                "import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;\n\n");
        Resource resource = request.getResourceResolver().getResource(path);
        Generator gen = new Generator(writer);
        if (resource.isResourceType("cq:Page")) gen.page(resource);
        else gen.resource(null, resource);
    }

    class Generator {
        private final PrintWriter writer;
        private static final String INDENT = "        ";
        private static final String CONTINUATION_INDENT = ",\n            ";

        Generator(PrintWriter writer) {
            this.writer = writer;
        }

        void page(Resource resource) {
            Resource contentResource = resource.getChild("jcr:content");
            writer.println(INDENT + "Page " + resource.getName() + " = context.create().page(\"" + resource.getPath() + "\", null" + props(contentResource) + ");\n");
            subresources(contentResource);
        }

        void resource(String pathspec, Resource resource) {
            String patharg = pathspec != null ? pathspec + ", " + "\""  + resource.getName() + "\"" : "\"" + resource.getPath() + "\"";
            writer.println(INDENT + "Resource " + resource.getName() + " = context.create().resource(" + patharg + props(resource) + ");\n");
            subresources(resource);
        }

        void subresources(Resource resource) {
            for (Resource child : resource.getChildren()) {
                resource(resource.getName(), child);
            }
        }

        final Collection<String> ignored = Arrays.asList(JCR_PRIMARYTYPE, PROPERTY_RESOURCE_TYPE, "original_fragment_uuid");

        String props(Resource resource) {
            StringBuilder buf = new StringBuilder();
            ValueMap vm = resource.getValueMap();
            String primaryType = vm.get(JCR_PRIMARYTYPE, String.class);
            if (!"nt:unstructured".equals(primaryType)) {
                buf.append(CONTINUATION_INDENT).append("JCR_PRIMARYTYPE, ");
                appendString(buf, primaryType);
            }
            String resourceType = vm.get(PROPERTY_RESOURCE_TYPE, String.class);
            if (StringUtils.isNotBlank(resourceType)) {
                buf.append(CONTINUATION_INDENT).append("PROPERTY_RESOURCE_TYPE, ");
                appendString(buf, resourceType);
            }
            for (Map.Entry<String, Object> entry : vm.entrySet()) {
                String key = entry.getKey();
                if (!ignored.contains(key) && !key.startsWith("jcr:created") && !key.startsWith("jcr:lastModified") && !key.startsWith("cq:last")) {
                    Object value = entry.getValue();
                    if (value instanceof String[]) {
                        List<String> values = new ArrayList<String>(Arrays.asList((String[]) value));
                        if ("jcr:mixinTypes".equals(key)) {
                            values.remove("cq:LiveRelationship");
                        }
                        if (values.size() > 0) {
                            buf.append(CONTINUATION_INDENT);
                            appendString(buf, key);
                            buf.append(", ");
                            buf.append("new String[]{");
                            buf.append(values.stream()
                                    .map(s -> "\"" + s + "\"")
                                    .collect(Collectors.joining(", ")));
                            buf.append("}");
                        }
                    } else {
                        buf.append(CONTINUATION_INDENT);
                        appendString(buf, key);
                        buf.append(", ");
                        append(buf, value);
                    }
                }
            }
            return buf.toString();
        }

        void appendString(StringBuilder buf, String value) {
            if (StringUtils.isBlank(value)) {
                buf.append("null");
            } else {
                buf.append("\"").append(value.replaceAll("\"", "\\\"")).append("\"");
            }
        }

        void append(StringBuilder buf, Object value) {
            if (value instanceof String) {
                appendString(buf, (String) value);
            } else {
                appendString(buf, value.toString());
            }
        }
    }

    @ObjectClassDefinition(
            name = "Composum Nodes JCR to Test Setup Servlet",
            description = "This servlet serves as code generator to reproduce a part of the JCR tree easily in a unittest using SlingContext.\n" +
                    " \n" +
                    " CAUTION: not suitable for production, only for internal testing systems!\n" +
                    " \n" +
                    " Usage in the browser with e.g. http://localhost:6502/bin/cpm/nodes/debug/jcrtotestsetup.html/{path}\n"
    )
    public @interface Configuration {
        @AttributeDefinition(
                description = "Enable the servlet"
        )
        boolean enabled() default false;
    }

}
