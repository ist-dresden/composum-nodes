package com.composum.nodes.debugutil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This gets an Object as argument and recursively dumps all properties into a JSON.
 * Not threadsafe.
 * <p>Use in a JSP e.g.:
 * <pre>
 * DUMP:
 * <%= new com.composum.nodes.debugutil.YamlObjectDumper().get(pageContext.getAttribute("variable")) %>
 *                     </pre>
 * </p>
 */
public class YamlObjectDumper extends AbstractMap<Object, String> {

    protected StringBuilder sb = new StringBuilder();
    protected final String indentStep = "  ";
    /**
     * To avoid recursion we keep what's already written.
     */
    protected IdentityHashMap<Object, Object> seen = new IdentityHashMap<>();

    public boolean printLineNumbers = true;

    public String dump(Object o, int maxdepth) {
        sb.setLength(0);
        dump(o, maxdepth, "");
        return sb.toString();
    }

    /**
     * For access with Sightly / HTL: create variable with this and (ab-)use it as a map.
     */
    @Override
    public String get(Object key) {
        return dump(key, 10);
    }

    @Override
    public Set<Entry<Object, String>> entrySet() {
        return Collections.emptySet();
    }

    protected void dump(Object o, int maxdepth, String indent) {
        if (o == null) {
            sb.append("null");
            return;
        }
        if (o instanceof String) {
            sb.append('"').append(((String) o).replaceAll("\"", "\\\\\"")).append('"');
            return;
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
            return;
        }
        if (seen.containsKey(o)) {
            sb.append("\"object was already seen at line " + seen.get(o) + "\"");
            return;
        }
        seen.put(o, lineNumber());
        if (maxdepth < 0) {
            sb.append("...");
        } else if (o instanceof Class) {
            sb.append("\"class ").append(((Class) o).getName()).append("\"");
        } else if (o instanceof Iterable) { // represented with yaml - construct (leading dashes)
            for (Object item : (Iterable) o) {
                endLineAndIndent(indent);
                sb.append("- ");
                dump(item, maxdepth - 1, indent + indentStep);
            }
        } else if (o instanceof Map) {
            Map map = (Map) o;
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            List<Map.Entry> entrySet = new ArrayList(Arrays.asList(map.entrySet().toArray()));
            Collections.sort(entrySet, (o1, o2) ->
                    o1.getKey().toString().compareTo(o2.getKey().toString()));
            for (Map.Entry entry : entrySet) {
                endLineAndIndent(indent);
                sb.append(entry.getKey()).append(": ");
                dump(entry.getValue(), maxdepth - 1, indent + indentStep);
            }
        } else if (o instanceof Date) { // format it as ISO
            sb.append(((Date) o).toInstant());
        } else if (o instanceof Object[]) {
            dump(Arrays.asList((Object[]) o), maxdepth, indent);
        } else { // iterate over all public methods
            List<Method> methods = new ArrayList<>();
            addMethods(o.getClass(), methods);
            Collections.sort(methods, (m1, m2) -> m1.getName().compareTo(m2.getName()));
            for (java.lang.reflect.Method method : methods) {
                // if the method comes from Object, ignore it.
                if (method.getParameterCount() == 0 && !method.getDeclaringClass().equals(Object.class)) {
                    String name = method.getName();
                    endLineAndIndent(indent);
                    sb.append(name).append(": ");
                    try {
                        Object value = method.invoke(o);
                        dump(value, maxdepth - 1, indent + indentStep);
                    } catch (Exception e) {
                        sb.append(e.getMessage());
                    }
                }
            }
        }
    }

    protected void addMethods(Class<?> aClass, List<Method> methods) {
        // if class is public just add the public methods
        if (Modifier.isPublic(aClass.getModifiers())) {
            Stream.of(aClass.getMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .filter(m -> m.getParameterCount() == 0)
                    .filter(m -> !Modifier.isStatic(m.getModifiers()))
                    .filter(m -> !isMethodOverriddenFromObject(m))
                    .forEach(methods::add);
        } else {
            // if class is not public, we find the methods of the public interfaces
            // and add them to the list
            Stream.of(aClass.getInterfaces())
                    .forEach(i -> addMethods(i, methods));
        }
    }


    public static boolean isMethodOverriddenFromObject(Method method) {
        try {
            Object.class.getMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    protected final Pattern onlyNewlineAndSpace = Pattern.compile("\n *$");

    private void endLineAndIndent(String indent) {
        if (sb.length() == 0) {
            return;
        }
        if (onlyNewlineAndSpace.matcher(sb).find()) {
            if (!sb.toString().endsWith("\n" + indent)) {
                while (sb.charAt(sb.length() - 1) == ' ') {
                    sb.setLength(sb.length() - 1);
                }
                sb.append(indent);
            }
        } else {
            if (printLineNumbers) {
                sb.append("\t\t\t#").append(lineNumber());
            }
            sb.append("\n").append(indent);
        }
    }

    public int lineNumber() {
        int lines = 1;
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

}
