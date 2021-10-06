package com.composum.nodes.service.references

import com.composum.sling.core.service.PathReferencesService

println "meta: " + getMetaClass()
println "log: " + log
println "binding: " + binding
binding.variables.each({ entry ->
    println "  " + entry.key + " = " + entry.value
})

def service = getService(PathReferencesService.class)

def options = new PathReferencesService.Options()
        .basePath('/content/ist/composum/')
        //.propertyName('sling:resourceType')
        .includeChildren(true)
        .childrenOnly(false)
        .useAbsolutePath(true)
        .useRelativePath(false)
        .findRichText(true)
        //.resourceType('composum/pages/components/element/teaser')

def searchRoot = "/content/ist"
def path = "/content/ist/composum/home/pages/setup"

def referrers = service.findReferences(resourceResolver, options, searchRoot, path)
println 'path = "' + referrers.getPath() + '", absPath ="'+referrers.getAbsPath()+ '"'
println 'query: "' + referrers.getQueryString() + '"'
println 'references of (' + path + ')...'

while (referrers.hasNext()) {
    def ref = referrers.next()
    println ' - ' + ref.getResource().getPath()
    for (def name : ref.getPropertyNames()) {
        def property = ref.getProperty(name)
        println "   " + property.getName() + " (" + (property.isRichText()?'rich':'plain') + ") = ["
        for (def value : property.getValues()) {
            println "      '" + value.text + "', " + (value.absolute?'A':'') + (value.relative?'R':'') + (value.childPath?'C':'')
            for (def found : value.getPath()) {
                println "          - '" + found + "'"
            }
        }
        println "   ]"
    }
}

println 'no more references.'
