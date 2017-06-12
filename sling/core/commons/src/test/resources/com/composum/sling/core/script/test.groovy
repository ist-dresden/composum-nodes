package com.composum.sling.core.script

println "meta: " + getMetaClass()
println "log: " + log
println "binding: " + binding
binding.variables.each({ entry ->
    println "  " + entry.key + " = " + entry.value
});

info (this, ' and some text')

debug getService(GroovyShell.class)

print "test groovy ended."
