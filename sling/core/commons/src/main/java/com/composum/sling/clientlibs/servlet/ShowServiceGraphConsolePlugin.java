package com.composum.sling.clientlibs.servlet;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;


/**
 * Plots a graph of service usages. Use as console plugin: http://localhost:9090/system/console/servicegraph.
 * Use with graphviz tools http://graphviz.org/ e.g. with <br/> <code>
 * curl -u admin:admin 'http://localhost:9090/system/console/servicegraph.dot?classregex=%5Ecom.composum&type=dotty&bundle=true' | ccomps -x | unflatten -f -l 6 -c 3 | dot | gvpack | neato -Tpng -n2  > $TMPDIR/services.png ; open $TMPDIR/services.png
 * </code>
 * @see "https://github.com/magjac/d3-graphviz"
 */
@Component(label = "Composum Service Graph Webconsole Plugin",
        description = "Prints a dotty file describing the service relations")
@Service(value = Servlet.class)
@Properties({
        @Property(name = "felix.webconsole.label", value = "servicegraph"),
        @Property(name = "felix.webconsole.title", value = "Service Graph"),
        @Property(name = "felix.webconsole.category", value = "Composum"),
        @Property(name = "felix.webconsole.css", value = "clientlibs/" + ShowServiceGraphConsolePlugin.LOC_CSS),
})
public class ShowServiceGraphConsolePlugin extends HttpServlet {

    /** A regex to limit the service classes to. Default: <code>^com.composum</code>. */
    public static final String PARAM_CLASSREGEX = "classregex";

    /** Parameter that gives the type of the output: text or dotty / gv. Default: dotty. */
    public static final String PARAM_TYPE = "type";

    /** Parameter that determines wether we should insert the bundles as subgraphs in dotty. */
    public static final String PARAM_BUNDLE = "bundle";

    /** Location for the CSS. */
    protected static final String LOC_CSS = "slingconsole/composumplugin.css";

    protected BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    protected void writeForm(PrintWriter writer, HttpServletRequest request, String classRegex) {
        writer.println("<html><body><h2>Service reference structure</h2>");
        writer.println("<p>This shows the structure of the service crossreferences of a part of the application(s).</p>");
        writer.println("<form action=\"" + request.getRequestURL() + "\" method=\"get\">");
        writer.println("Show services with classnames matching regex: <input type=\"text\" size=\"80\" name=\"classregex\" value=\"" + classRegex + "\">\n" +
                "<br>\n" +
                "Show as \n" +
                "  <input type=\"radio\" name=\"type\" value=\"graph\" checked> graph with <a href=\"https://github.com/magjac/d3-graphviz\">d3-graphviz</a>\n" +
                "  <input type=\"radio\" name=\"type\" value=\"dotty\"> dotty for <a href=\"http://graphviz.org/\">Graphviz</a>\n" +
                "  <input type=\"radio\" name=\"type\" value=\"text\"> Text. " +
                "  Do <input type=\"radio\" name=\"bundle\" value=\"true\" checked> group / \n" +
                "  <input type=\"radio\" name=\"bundle\" value=\"false\"> do not group services into bundles.\n" +
                "  <input type=\"submit\">\n");
        writer.println("</form>");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().endsWith(LOC_CSS)) {
            response.setContentType("text/css");
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("/" + LOC_CSS),
                    response.getOutputStream());
            return;
        }

        String type = StringUtils.defaultIfBlank(request.getParameter(PARAM_TYPE), "graph").toLowerCase();
        boolean isText = type.equals("text");
        boolean isGraph = type.equals("graph");
        boolean isConsole = request.getRequestURI().contains("console");
        boolean showBundles = StringUtils.defaultIfBlank(request.getParameter(PARAM_BUNDLE), "true").toLowerCase().equals("true");
        String classRegex = StringUtils.defaultIfBlank(request.getParameter(PARAM_CLASSREGEX), "^com.composum");
        Pattern classPattern = Pattern.compile(classRegex);

        PrintWriter writer = response.getWriter();
        if (isConsole && !request.getRequestURI().endsWith(".txt") && !request.getRequestURI().endsWith(".dot") && !request.getRequestURI().endsWith(".gv")) {
            response.setContentType("text/html");
            writeForm(writer, request, classRegex);
            if (isGraph) {
                writer.println("You can scroll around the graph by dragging it with the mouse. Within the graph, the scroll wheel works as zoom in / zoom out.");
                writer.println("<div id=\"graph\" style=\"text-align: center;\"></div>\n");
            }
            writer.println("<pre id=\"digraph\">");
        } else if (isText) {
            response.setContentType("text/plain");
        } else {
            response.setContentType("text/vnd.graphviz");
        }
        if (!isText)
            writer.println(" digraph servicestructure {");

        Map<Class<?>, ServiceReference<?>> classes = new HashMap<>();

        try {
            ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(null, null);
            Arrays.sort(refs, new Comparator<ServiceReference<?>>() {
                @Override
                public int compare(ServiceReference<?> o1, ServiceReference<?> o2) {
                    return ComparatorUtils.naturalComparator().compare(o1.toString(), o2.toString());
                }
            });
            for (ServiceReference<?> ref : refs) {
                Object service = null;
                Class<?> clazz;
                try {
                    service = bundleContext.getService(ref);
                    if (service == null) {
                        if (isText) writer.println(ref + " : no service");
                        continue;
                    }
                    clazz = service.getClass();
                } finally {
                    if (service != null) {
                        service = null;
                        bundleContext.ungetService(ref);
                    }
                }
                if (!classPattern.matcher(clazz.getName()).find())
                    continue;
                if (isText) {
                    writer.println(ref.toString());
                    writer.println("class: " + clazz.getName());
                    writer.println("from bundle " + ref.getBundle().getSymbolicName());
                    for (String propertyKey : ref.getPropertyKeys()) {
                        Object property = ref.getProperty(propertyKey);
                        if (property.getClass().isArray()) {
                            Object[] array = (Object[]) property;
                            property = Arrays.asList(array).toString();
                        }
                        writer.println("    " + propertyKey + "=" + property);
                    }
                } else {
                    classes.put(clazz, ref);
                }
            }

            if (!isText) {
                writeReferences(writer, classes, classPattern, showBundles);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        if (!isText) writer.println("}");
        if (isConsole) {
            writer.println("</pre>");
            if (isGraph) writer.println("<script src=\"https://d3js.org/d3.v4.min.js\"></script>\n" +
                    "<script src=\"https://unpkg.com/viz.js@1.8.0/viz.js\" type=\"javascript/worker\"></script>\n" +
                    "<script src=\"https://unpkg.com/d3-graphviz@1.4.0/build/d3-graphviz.min.js\"></script>\n" +
                    "<script>\n" +
                    "dot = document.getElementById('digraph').innerText;\n" +
                    "d3.select(\"#graph\").graphviz().fade(false).renderDot(dot);\n" +
                    "</script>");
            writer.println("</html>");
        }
    }

    protected void writeReferences(PrintWriter writer, Map<Class<?>, ServiceReference<?>> classes, Pattern classPattern, boolean showBundles) {
        Map<String, Class<?>> classIdx = new TreeMap<>();
        for (Class<?> clazz : classes.keySet()) {
            classIdx.put(clazz.getName(), clazz);
        }

        Map<String, String> shortnames = new TreeMap<>();
        String prefix = StringUtils.getCommonPrefix(classIdx.keySet().toArray(new String[0]));
        for (String cname : classIdx.keySet()) {
            String shortcname = StringUtils.removeStart(cname, prefix);
            String pkg = StringUtils.substringBeforeLast(shortcname, ".");
            String cls = StringUtils.substringAfterLast(shortcname, ".");
            shortnames.put(cname, pkg + "\\n" + cls);
        }

        Set<String> bundles = new TreeSet<>();
        for (Map.Entry<Class<?>, ServiceReference<?>> entry : classes.entrySet()) {
            String bundleName = entry.getValue().getBundle().getSymbolicName();
            bundles.add(bundleName);
        }

        for (String bundle : bundles) {
            if (showBundles) {
                writer.println("    subgraph cluster_" + bundle.replaceAll("[^a-zA-Z0-9]+", "_") + " {");
                writer.println("        graph[style=dashed];");
                writer.println("        label=\"" + bundle + "\";");
            }

            for (Map.Entry<String, Class<?>> classEntry : classIdx.entrySet()) {
                Class<?> clazz = classEntry.getValue();
                String bundlename = classes.get(clazz).getBundle().getSymbolicName();
                if (!bundle.equals(bundlename))
                    continue;

                Class<?> serviceOrSuper = clazz;
                Set<String> refFields = new TreeSet<>();
                while (serviceOrSuper != null) {
                    for (Field field : serviceOrSuper.getDeclaredFields()) {
                        Class<?> fieldClass = field.getType();
                        if (classPattern.matcher(fieldClass.getName()).find()) {
                            for (Class<?> serviceClass : classIdx.values()) {
                                if (fieldClass.isAssignableFrom(serviceClass)) {
                                    refFields.add(serviceClass.getName());
                                    break;
                                }
                            }
                        }
                    }
                    serviceOrSuper = serviceOrSuper.getSuperclass();
                }

                for (String field : refFields) {
                    writer.println("        \"" + shortnames.get(clazz.getName()) + "\" -> \"" + shortnames.get(field) + "\";");
                }
            }

            if (showBundles) {
                writer.println("    }");
            }
        }
    }

}
