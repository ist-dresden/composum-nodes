package com.composum.sling.core.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * an annotation to mark a model class which instances are built by a factory service
 * this supports model instantiation from inner classes of a service (e.g. for security reasons)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanFactory {

    /**
     * The service class of the factory.
     */
    Class<? extends SlingBeanFactory> serviceClass();
}
