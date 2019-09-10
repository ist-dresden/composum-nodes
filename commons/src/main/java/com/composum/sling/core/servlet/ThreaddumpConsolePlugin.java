package com.composum.sling.core.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Displays the stacktraces of active or all threads. Use as console plugin:
 * http://localhost:9090/system/console/threaddump
 */
@Component(label = "Composum Threaddump Webconsole Plugin",
        description = "Prints stacktraces for all threads")
@Service(value = Servlet.class)
@Properties({
        @Property(name = "felix.webconsole.label", value = "threaddump"),
        @Property(name = "felix.webconsole.title", value = "Threaddump"),
        @Property(name = "felix.webconsole.category", value = "Composum"),
        @Property(name = "felix.webconsole.css", value = "threaddump/" + ThreaddumpConsolePlugin.LOC_CSS),
})
public class ThreaddumpConsolePlugin extends HttpServlet {

    /** Location for the CSS. */
    protected static final String LOC_CSS = "slingconsole/threaddumpplugin.css";

    public static final String PARAM_STATE = "state";
    public static final String PARAM_NAMEREGEX = "nameregex";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        if (request.getRequestURI().endsWith(LOC_CSS)) {
            response.setContentType("text/css");
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("/" + LOC_CSS),
                    response.getOutputStream());
            return;
        }

        PrintWriter writer = response.getWriter();
        response.setContentType("text/html");

        writer.print("<html><body><h2>Thread dump</h2>");
        new ThreaddumpRunner(writer, request).print();
        writer.println("</body></html>");
    }

    protected class ThreaddumpRunner {
        protected final PrintWriter writer;
        protected final HttpServletRequest request;
        protected Set<Thread.State> stati = Collections.singleton(Thread.State.RUNNABLE);
        protected String nameRegexStr = "";
        protected Pattern nameRegex = Pattern.compile(nameRegexStr);

        public ThreaddumpRunner(PrintWriter writer, HttpServletRequest request) {
            this.writer = writer;
            this.request = request;

            String[] statiArray = request.getParameterValues(PARAM_STATE);
            if (statiArray != null && statiArray.length > 0) {
                stati = new HashSet<>();
                for (String stateStr : statiArray) {
                    Thread.State state = Thread.State.valueOf(stateStr);
                    stati.add(state);
                }
            }

            if (StringUtils.isNotBlank(request.getParameter(PARAM_NAMEREGEX))) {
                nameRegexStr = request.getParameter(PARAM_NAMEREGEX);
                try {
                    nameRegex = Pattern.compile(nameRegexStr);
                } catch (PatternSyntaxException e) {
                    writer.println("<p><strong>Regex syntax error: " + e + "</strong></p>");
                }
            }
        }

        public void print() {
            printForm();
            writer.println("<p>Since many threads share the same stacktrace, threads with the " +
                    "same stacktrace are grouped together.</p><br>");
            printThreads();
        }

        protected void printThreads() {
            writer.println("<ul>");
            Map<Thread, StackTraceElement[]> traceMap = Thread.getAllStackTraces();

            List<Pair<Thread, String>> traces = traceMap.entrySet().stream()
                    .map((e) -> Pair.of(e.getKey(), stackTrace(e.getValue())))
                    .filter((e) -> nameRegex.matcher(e.getLeft().getName()).find())
                    .filter((e) -> stati.contains(e.getLeft().getState()))
                    .collect(Collectors.toList());
            Collections.sort(traces, Comparator.comparing((e) -> e.getLeft().getName()));

            // Since many many traces have the same stacktrace, we group them by stacktrace.
            Map<String, List<Pair<Thread, String>>> tracesGrouped = new TreeMap<>(traces.stream()
                    .collect(Collectors.groupingBy((e) -> e.getRight())));
            List<Pair<String, List<Thread>>> tracesGroupedByStacktrace = tracesGrouped.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), extractThreads(e.getValue())))
                    .collect(Collectors.toList());
            Collections.sort(tracesGroupedByStacktrace, Comparator.comparing((e) -> e.getRight().get(0).getName()));

            for (Pair<String, List<Thread>> traceAndThreads : tracesGroupedByStacktrace) {
                writer.println("<li>");
                for (Thread t : traceAndThreads.getRight()) {
                    writer.print(t.getId());
                    writer.print(" (" + t.getState() + ")");
                    writer.print(" : ");
                    writer.print(t.getName());
                    writer.println("<br/>");
                }
                writer.println("<pre>");
                writer.println(traceAndThreads.getLeft());
                writer.println("</pre>");
                writer.println("</li>");
            }

            writer.println("</ul>");
        }

        private List<Thread> extractThreads(List<Pair<Thread, String>> threadTraces) {
            return threadTraces.stream()
                    .map((e) -> e.getLeft())
                    .collect(Collectors.toList());
        }

        protected String stackTrace(StackTraceElement[] stackTraceElements) {
            StringBuffer buf = new StringBuffer();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                buf.append("     at ")
                        .append(stackTraceElement.getClassName())
                        .append(".")
                        .append(stackTraceElement.getMethodName())
                        .append("(")
                        .append(stackTraceElement.getFileName())
                        .append(":")
                        .append(stackTraceElement.getLineNumber())
                        .append("\n");

            }
            return buf.toString();
        }

        protected void printForm() {
            writer.println("<form action=\"" + request.getRequestURL() + "\" method=\"get\">");
            writer.println("Print only threads of stati ");
            for (Thread.State value : Thread.State.values()) {
                String checked = stati.contains(value) ? "checked" : "";
                writer.println("  <input type=\"checkbox\" name=\"state\" value=\"" + value.name() + "\" "
                        + checked + "> " + value);
            }
            writer.println(" with names matching regex <input type=\"text\" name=\"nameregex\" value=\"" + nameRegex + "\">");
            writer.println(" <input type=\"submit\">\n");
            writer.println("</form>");
        }

    }
}
