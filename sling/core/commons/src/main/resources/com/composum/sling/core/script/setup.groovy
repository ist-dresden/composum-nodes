package com.composum.sling.core.script

import org.apache.sling.api.resource.ResourceResolver

import javax.jcr.query.Query

/**
 * the default setup script to prepare the groovy script 'script'
 * - can add some meta methods for the script and
 * - must return the additional/default binding variables for the script
 * executed by the runner with the variables
 * - script: the groovy Script object
 * - log: the logger used by the groovy runner
 * - out: the print writer from the calling context
 */

script.metaClass.save << { ->
    session.save()
}

script.metaClass.getNode << { path ->
    session.getNode(path)
}

script.metaClass.getResource << { path ->
    resourceResolver.getResource(path)
}

script.metaClass.commit << { ->
    resourceResolver.commit()
}

// Query (similar to CQ Groovyconsole)

Query.metaClass.setHitsPerPage << { value -> delegate.limit = value }

script.metaClass.buildQueryString << { Map predicates ->
    predicates = predicates.clone()
    StringBuilder query = new StringBuilder("/jcr:root")
    if (predicates['path']) {
        def path = predicates['path']
        query.append(path)
        if (!path.endsWith('/')) {
            query.append('/')
        }
    } else {
        query.append('/')
    }
    query.append('/')
    if (predicates['type']) {
        query.append('element(')
    }
    if (predicates['name']) {
        query.append(predicates['name'])
    } else {
        query.append('*')
    }
    if (predicates['type']) {
        query.append(',').append(predicates['type']).append(')')
    }
    predicates.remove('path')
    predicates.remove('name')
    predicates.remove('type')
    int size = predicates.size()
    if (size > 0) {
        query.append('[')
        predicates.eachWithIndex { entry, index ->
            if (entry.value) {
                if (entry.value.indexOf('%') >= 0) {
                    query.append('jcr:like(')
                    query.append(entry.key)
                    query.append(",'")
                    query.append(entry.value)
                    query.append("')")
                } else {
                    if ('.' == entry.key) {
                        query.append("jcr:contains(.,'")
                        query.append(entry.value)
                        query.append("')")
                    } else {
                        query.append('@')
                        query.append(entry.key)
                        query.append("='")
                        query.append(entry.value)
                        query.append("'")
                    }
                }
            } else {
                query.append('@')
                query.append(entry.key)
            }
            if (index < size-1) {
                query.append(' and ')
            }
        }
        query.append(']')
    }
    query.toString()
}

script.metaClass.createQuery << { Map predicates ->
    String query = script.buildQueryString(predicates)
    return queryManager.createQuery(query, Query.XPATH)
}

ResourceResolver.metaClass.findResources << { Map predicates ->
    String query = script.buildQueryString(predicates)
    return delegate.findResources(query, Query.XPATH)
}

// Service

script.metaClass.getService << { Class serviceClass ->
    def serviceRef = bundleContext.getServiceReference(serviceClass)
    serviceRef ? bundleContext.getService(serviceRef) : null
}

script.metaClass.getService << { String serviceClass ->
    def serviceRef = bundleContext.getServiceReference(serviceClass)
    serviceRef ? bundleContext.getService(serviceRef) : null
}

script.metaClass.getServices << { Class serviceClass, String filter ->
    def serviceRefs = bundleContext.getServiceReferences(serviceClass, filter)
    serviceRefs.collect { bundleContext.getService(it) }
}

script.metaClass.getServices << { String serviceClass, String filter ->
    def serviceRefs = bundleContext.getServiceReferences(serviceClass, filter)
    serviceRefs.collect { bundleContext.getService(it) }
}

script.metaClass.error << { ... args ->
    log.error(args ? args.join('') : '')
}

script.metaClass.warn << { ... args ->
    log.warn(args ? args.join('') : '')
}

script.metaClass.info << { ... args ->
    log.info(args ? args.join('') : '')
}

script.metaClass.debug << { ... args ->
    log.debug(args ? args.join('') : '')
}

[:]