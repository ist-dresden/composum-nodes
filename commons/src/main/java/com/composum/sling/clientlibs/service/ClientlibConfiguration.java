package com.composum.sling.clientlibs.service;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

public interface ClientlibConfiguration {

    @ObjectClassDefinition(
            name = "Composum Platform Access Filter Configuration"
    )
    @interface Config {

        @AttributeDefinition(
                name = "tagdebug",
                description = "Inserts HTML comments with the client libraries that have been called up into the pag"
        )
        boolean tagdebug() default false;

        @AttributeDefinition(
                name = "debug",
                description = "let files unchanged and unbundled if set to 'true'"
        )
        boolean debug() default false;

        @AttributeDefinition(
                name = "author.mapping.enabled",
                description = "if enabled the resolver mapping is used in author mode; default: false"
        )
        boolean enableAuthorMapping() default false;

        // CSS configuration

        @AttributeDefinition(
                name = "css.minimize",
                description = "compress with the YUI compressor (if not 'debug' is set); default: true"
        )
        boolean cssMinimize() default true;

        @AttributeDefinition(
                name = "css.lineBreak",
                description = "length of compressed CSS source lines (if not 'debug' is set); default: 0"
        )
        int cssLineBreak() default 0;

        @AttributeDefinition(
                name = "css.template",
                description = "the HTML template for the CSS link rendering"
        )
        String cssTemplate() default "  <link rel=\"stylesheet\" href=\"{0}\" />";

        // JS configuration

        @AttributeDefinition(
                name = "javascript.template",
                description = "the HTML template for the Javascript tag rendering"
        )
        String javascriptTemplate() default "  <script type=\"text/javascript\" src=\"{0}\"></script>";

        // Link rendering configuration

        @AttributeDefinition(
                name = "link.template",
                description = "the HTML template for the general link rendering"
        )
        String linkTemplate() default "  <link rel=\"{1}\" href=\"{0}\" />";

        // general configuration

        @AttributeDefinition(
                name = "gzip.enabled",
                description = "if 'true' the content is zippend if possible"
        )
        boolean gzipEnabled() default false;

        @AttributeDefinition(
                name = "clientlibs.cache.root",
                description = "the root folder for the Javascript clientlib cache"
        )
        String cacheRoot() default "/var/cache/clientlibs";

        @AttributeDefinition(
                name = "clientlibs.files.minified",
                description = "if 'on' for all clientlib files which have a '.min' sibling the '.min' files is used; default: 'on'"
        )
        boolean useMinifiedFiles() default true;

        @AttributeDefinition(
                name = "clientlibs.url.map",
                description = "if 'on' all clientlib URLs are mapped by the Resource Resolver; default: 'on'"
        )
        boolean mapClientlibURLs() default true;

        @AttributeDefinition(
                name = "clientlibs.threadpool.min",
                description = "the minimum size of the thread pool for clientlib processing (must be '10' or greater)"
        )
        int threadPoolMin() default 10;

        @AttributeDefinition(
                name = "clientlibs.threadpool.max",
                description = "the size (maximum) of the thread pool for clientlib processing (must be equal or greater than the minimum)"
        )
        int threadPoolMax() default 20;

        @AttributeDefinition(
                name = "clientlibs.resolver.cachetime",
                description = "the time (in seconds) the clientlib resolver caches the locations of all client libraries for a category. <=0 means no caching."
        )
        int resolverCachetime() default 60;
    }

    Config getConfig();
}
