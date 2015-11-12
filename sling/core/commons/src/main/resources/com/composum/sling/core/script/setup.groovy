package com.composum.sling.core.script

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

script.metaClass.getService << { Class serviceClass ->
    def serviceRef = bundleContext.getServiceReference(serviceClass)
    bundleContext.getService(serviceRef)
}

script.metaClass.getService << { String serviceClass ->
    def serviceRef = bundleContext.getServiceReference(serviceClass)
    bundleContext.getService(serviceRef)
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
    log.error(args.join(''))
}

script.metaClass.warn << { ... args ->
    log.warn(args.join(''))
}

script.metaClass.info << { ... args ->
    log.info(args.join(''))
}

script.metaClass.debug << { ... args ->
    log.debug(args.join(''))
}

[:]