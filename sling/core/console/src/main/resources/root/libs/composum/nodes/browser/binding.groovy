package root.libs.composum.nodes.browser

response.setContentType('text/html');
println "<h4>Groovy Script Binding</h4>"
println "<dl>"
println "<dt>binding</dt><dd>${binding}</dd>"
println "<dt>variables</dt><dd>${binding.variables instanceof java.util.Map ? 'isMap!' : '???'}</dd>"
binding.variables.each {
    println "<dt>${it.key}</dt><dd>${it.value}</dd>"
}
println "</dl>"
