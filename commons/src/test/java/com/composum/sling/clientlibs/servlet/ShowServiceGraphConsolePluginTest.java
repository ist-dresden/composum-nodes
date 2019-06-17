package com.composum.sling.clientlibs.servlet;

import com.composum.sling.clientlibs.service.ClientlibService;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Tests for {@link ShowServiceGraphConsolePlugin}. */
public class ShowServiceGraphConsolePluginTest {

    private String something;
    private List<Double> doubleList;
    private Float floatArray[];
    private WeakReference<java.lang.RuntimeException> exceptionArray[];
    private Map<Integer, Character> map;
    private AtomicReference<Boolean> typed;
    private Subtyped subtyped;

    private static class Subtyped extends AtomicReference<java.lang.Exception> {
    }

    @Test
    public void collectReferredClasses() {
        List<Class> refClasses = new ArrayList<>();
        Set<Type> visited = new HashSet<>();
        for (Field declaredField : ShowServiceGraphConsolePluginTest.class.getDeclaredFields())
            ShowServiceGraphConsolePlugin.collectReferredClasses(declaredField.getGenericType(), refClasses, visited);
        Collections.sort(refClasses, new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });
        List<String> classNames = new ArrayList<>();
        for (Class refClass : refClasses)
            classNames.add(refClass.getSimpleName());
        // Constable, ConstantDesc, is an uninteresting difference between JDKs.
        Assert.assertEquals(classNames.toString().replace("Constable, ConstantDesc, ", ""), "[AtomicReference, Boolean, CharSequence, Character, Collection, Comparable, Double, Exception, Float, Integer, Iterable, List, Map, Number, Object, Reference, RuntimeException, Serializable, String, Subtyped, Throwable, WeakReference]");
    }

}
