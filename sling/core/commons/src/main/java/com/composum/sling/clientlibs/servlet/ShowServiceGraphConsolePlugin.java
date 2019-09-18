package com.composum.sling.clientlibs.servlet;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 *
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

    /** Parameter that determines whether we should insert the bundles as subgraphs in dotty. */
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
        writer.println("<p>This shows the structure of the service crossreferences of a part of the application(s). Please be aware that this is not 100% complete - " +
                "it reconstructs the usage by the types of the fields of the services as read out by Java reflection.</p>");
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
            writer.println("<h3 id='thegraph'>Graph</h3>");
            if (isGraph) {
                writer.println("<p>You can scroll around the graph by dragging it with the mouse. Within the graph, " +
                        "the scroll wheel works as zoom in / zoom out. <button onclick='maximizeGraph();'>Maximize " +
                        "graph</button>\n" +
                        "<a href=\"#asimage\">Go to saveable SVG</a>" +
                        "<p>&nbsp;");
                writer.println("<div id=\"graph\" style=\"text-align: center;\"><div id=\"wait\" style=\"padding: 10px;\">(Please wait for rendering process.)</div></div>\n");
            }
            writer.println("<pre id=\"digraph\">");
        } else if (isText) {
            response.setContentType("text/plain");
        } else {
            response.setContentType("text/vnd.graphviz");
        }
        if (!isText) { writer.println(" digraph servicestructure {"); }

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
                        if (isText) { writer.println(ref + " : no service"); }
                        continue;
                    }
                    clazz = service.getClass();
                } finally {
                    if (service != null) {
                        service = null;
                        bundleContext.ungetService(ref);
                    }
                }
                if (!classPattern.matcher(clazz.getName()).find()) { continue; }
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
        if (!isText) { writer.println("}"); }
        if (isConsole) {
            writer.println("</pre>");
            if (isGraph) {
                writer.println("<h3 id=\"asimage\">Graph as image</h3>");
                writer.println("<p>Here's the graph as SVG image (shown here in reduced size). Open or save the image" +
                        " using the browsers context menu.</p>");
                writer.println("<img id=\"saveimg\" style=\"max-width:256px;max-height:256px;min-width: 32px;min-height: 32px;border: 1px solid #000;\" />");
                writer.println("<script src=\"https://d3js.org/d3.v4.min.js\"></script>\n" +
                        "<script src=\"https://unpkg.com/viz.js@1.8.0/viz.js\" type=\"javascript/worker\"></script>\n" +
                        "<script src=\"https://unpkg.com/d3-graphviz@1.4.0/build/d3-graphviz.min.js\"></script>\n" +
                        "<script>\n" +
                        "function svg2img() {\n" +
                        "    var svg = document.querySelector('svg');\n" +
                        "    var xml = new XMLSerializer().serializeToString(svg);\n" +
                        "    var svg64 = btoa(xml);\n" +
                        "    var b64start = 'data:image/svg+xml;base64,';\n" +
                        "    var image64 = b64start + svg64;\n" +
                        "    return image64;\n" +
                        "};\n" +
                        "\n" +
                        "\n" +
                        "function createSvg() {\n" +
                        "    try {\n" +
                        "        document.getElementById('wait').style.display='none';\n" +
                        "        img = document.getElementById('saveimg');\n" +
                        "        img.src = svg2img();\n" +
                        "    }\n" +
                        "    catch (e) {\n" +
                        "        if (console) console.log(e);\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dot = document.getElementById('digraph').innerText;\n" +
                        "graphviz = d3.select(\"#graph\").graphviz();\n" +
                        "graphviz.on('end',createSvg);\n" +
                        "graphviz.renderDot(dot);\n" +
                        "\n" +
                        "function maximizeGraph() {\n" +
                        "    $('svg').css('width', window.innerWidth).css('height', window.innerHeight).css('position', 'fixed').css('left', 0).css('top', 0)\n" +
                        "}\n" +
                        "</script>");
            }
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
                if (!bundle.equals(bundlename)) { continue; }

                Class<?> serviceOrSuper = clazz;
                List<Class> referredClasses = new ArrayList<>();
                while (serviceOrSuper != null) {
                    for (Field field : serviceOrSuper.getDeclaredFields()) {
                        collectReferredClasses(field.getGenericType(), referredClasses, new HashSet<Type>());
                    }
                    serviceOrSuper = serviceOrSuper.getSuperclass();
                }
                Set<String> refFields = new TreeSet<>();
                for (Class referredClass : referredClasses) {
                    if (classPattern.matcher(referredClass.getName()).find()) {
                        for (Class<?> serviceClass : classIdx.values()) {
                            if (referredClass.isAssignableFrom(serviceClass)) {
                                refFields.add(serviceClass.getName());
                                break;
                            }
                        }
                    }
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

    protected static void collectReferredClasses(Type type, List<Class> referredClasses, Set<Type> visited) {
        if (visited.contains(type) || type == null) {
            return; // infinite recursion brake
        }
        visited.add(type);

        if (type instanceof Class) {
            Class clazz = (Class) type;
            if (clazz.isArray()) {
                referredClasses.add(clazz.getComponentType());
            } else {
                referredClasses.add(clazz);
                collectReferredClasses(clazz.getGenericSuperclass(), referredClasses, visited);
                for (Type genericInterface : clazz.getGenericInterfaces()) {
                    collectReferredClasses(genericInterface, referredClasses, visited);
                }
            }
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            collectReferredClasses(TypeUtils.getArrayComponentType(arrayType), referredClasses, visited);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            collectReferredClasses(parameterizedType.getRawType(), referredClasses, visited);
            collectReferredClasses(parameterizedType.getOwnerType(), referredClasses, visited);
            for (Type typeArgument : TypeUtils.getTypeArguments(parameterizedType).values()) {
                collectReferredClasses(typeArgument, referredClasses, visited);
            }
        }
    }

}
