package com.composum.sling.core.script

script.metaClass.getService << { serviceClass ->
    "getService(" + serviceClass + ") invoked"
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

[
    aVariable : 123.4
]