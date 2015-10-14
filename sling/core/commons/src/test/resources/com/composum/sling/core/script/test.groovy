package com.composum.sling.core.script

/**
 * Created by rw on 08.10.15.
 */

println "meta: " + getMetaClass()
println "logger: " + logger
println "binding: " + binding
binding.variables.each({ entry ->
    println "  " + entry.key + " = " + entry.value
});

info (this, ' and some text')

debug getService(GroovyShell.class)

print "test groovy ended."
