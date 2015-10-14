package com.composum.sling.core.script

script.metaClass.getService << { serviceClass ->
    "getService(" + serviceClass + ") invoked"
}

script.metaClass.error << { ... args ->
    logger.error(args.join(''))
}

script.metaClass.warn << { ... args ->
    logger.warn(args.join(''))
}

script.metaClass.info << { ... args ->
    logger.info(args.join(''))
}

script.metaClass.debug << { ... args ->
    logger.debug(args.join(''))
}

[
    aVariable : 123.4
]