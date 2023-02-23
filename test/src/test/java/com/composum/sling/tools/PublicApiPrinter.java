package com.composum.sling.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.jackrabbit.vault.packaging.registry.PackageRegistry;

/**
 * Tool to explore the public API transitively starting from a single class / interface by introspection.
 * Usage: call with arguments (regex matching all considered classes) ,
 * (package string to remove from printing) , (classname of starting class)  e.g.:<br/>
 * <code>org.apache.jackrabbit.vault.* org.apache.jackrabbit.vault. org.apache.jackrabbit.vault.packaging.registry.PackageRegistry</code>
 */
@SuppressWarnings("scwjava_RegexInjection")
public class PublicApiPrinter {
    private Pattern introspectRegex;
    private Queue<Class> classesToPrint = new ArrayDeque<>();
    private Set<Class> printedClasses = new HashSet<>();
    private boolean printMethods = true;
    private String removeString;
    private Pattern ignoredMethodNames = Pattern.compile("toString|equals|hashCode|compareTo");
    private Pattern ignoredDeclaringClass = Pattern.compile("java.lang.Object");

    public static void main(String[] args) throws Exception {
        String classname = args[2];
        try {
            Class clazz = PublicApiPrinter.class.getClassLoader().loadClass(classname);
            String regex = args[0];
            String removeString = args[1];

            PublicApiPrinter printer = new PublicApiPrinter();
            printer.introspectRegex = Pattern.compile(regex);
            printer.printMethods = false;
            printer.removeString = removeString;
            System.out.println("regex=" + printer.introspectRegex);
            System.out.println("remove=" + printer.removeString);
            System.out.println();

            printer.considerClass(clazz);
            printer.printApi();

            System.out.println();
            System.out.println("##########################################################################");
            System.out.println();

            printer = new PublicApiPrinter();
            printer.introspectRegex = Pattern.compile(regex);
            printer.removeString = removeString;
            printer.printMethods = true;
            printer.considerClass(clazz);
            printer.printApi();

        } catch (Exception e) {
            throw new Exception("Trouble printing public API of " + classname, e);
        }
    }

    private void considerClass(Class clazz) {
        if (introspectRegex.matcher(clazz.toString()).find() && !printedClasses.contains(clazz)
                && !clazz.isArray() && !clazz.isEnum() && !clazz.isLocalClass() && !clazz.isAnonymousClass()
                && !clazz.isAnnotation() && !clazz.isMemberClass() && !clazz.isPrimitive() && !clazz.isSynthetic()) {
            classesToPrint.add(clazz);
            printedClasses.add(clazz);
        }
    }

    private void printApi() {
        while (!classesToPrint.isEmpty()) {
            Class clazz = classesToPrint.remove();

            System.out.println("##### " + clazz + " #####");
            Method[] methods = clazz.getMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())
                        || this.ignoredMethodNames.matcher(method.getName()).matches()
                        || this.ignoredDeclaringClass.matcher(method.getDeclaringClass().toString()).find()
                ) continue;
                if (printMethods) {
                    String descr = method.toString();
                    descr = StringUtils.replaceAll(descr, Pattern.quote(removeString), "");
                    descr = StringUtils.replaceAll(descr, "public |abstract ", "");
                    descr = StringUtils.replaceAll(descr, " throws .*", "");
                    System.out.println(descr);
                }
                considerClass(method.getReturnType());
                Arrays.stream(method.getParameterTypes()).forEach(this::considerClass);
            }
            if (printMethods) System.out.println();
        }
    }
}
