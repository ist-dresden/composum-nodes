package com.composum.sling.clientlibs.service;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

public interface ClientlibConfiguration {

    @ObjectClassDefinition(
            name = "Composum Nodes Clientlib Configuration"
    )
    @interface Config {

        @AttributeDefinition(
                name = "Debug Mode",
                description = "let files unchanged and unbundled if set to 'true'"
        )
        boolean debug() default false;

        @AttributeDefinition(
                name = "Mapped Author URIs",
                description = "if enabled the resolver mapping is used in author mode; default: false"
        )
        boolean author_mapping_enabled() default false;

        // CSS configuration

        @AttributeDefinition(
                name = "Minimize CSS",
                description = "compress with the YUI compressor (if not 'debug' is set); default: true"
        )
        boolean css_minimize() default true;

        @AttributeDefinition(
                name = "CSS line break",
                description = "length of compressed CSS source lines (if not 'debug' is set); default: 0"
        )
        int css_line_break() default 0;

        @AttributeDefinition(
                name = "Debug Tag",
                description = "Inserts HTML comments with the client libraries that have been called up into the page"
        )
        boolean debug_tag() default false;

        @AttributeDefinition(
                name = "CSS link template",
                description = "the HTML template for the CSS link rendering"
        )
        String template_link_css() default "  <link rel=\"stylesheet\" href=\"{0}\" />";

        // JS configuration

        @AttributeDefinition(
                name = "Javascript template",
                description = "the HTML template for the Javascript tag rendering"
        )
        String template_link_javascript() default "  <script type=\"text/javascript\" src=\"{0}\"></script>";

        // Link rendering configuration

        @AttributeDefinition(
                name = "general Link template",
                description = "the HTML template for the general link rendering"
        )
        String template_link_general() default "  <link rel=\"{1}\" href=\"{0}\" />";

        // general configuration

        @AttributeDefinition(
                name = "GZip enabled",
                description = "if 'true' the content is zippend if possible"
        )
        boolean gzip_enabled() default false;

        @AttributeDefinition(
                name = "Cache root",
                description = "the root folder for the Javascript clientlib cache"
        )
        String clientlibs_cache_root() default "/var/composum/clientlibs";

        @AttributeDefinition(
                name = "Use minified variation",
                description = "if 'on' for all clientlib files which have a '.min' sibling the '.min' files is used; default: 'on'"
        )
        boolean clientlibs_minified_use() default true;

        @AttributeDefinition(
                name = "Map Clientlib URIs",
                description = "if 'on' all clientlib URLs are mapped by the Resource Resolver; default: 'on'"
        )
        boolean clientlibs_url_map() default true;

        @AttributeDefinition(
                name = "Resolver Cache Time",
                description = "the time (in seconds) the clientlib resolver caches the locations of all client libraries for a category. <=0 means no caching."
        )
        int clientlibs_resolver_cache_time() default 60;

        @AttributeDefinition(
                name = "Threadpool min",
                description = "the minimum size of the thread pool for clientlib processing (must be '10' or greater)"
        )
        int clientlibs_threadpool_min() default 10;

        @AttributeDefinition(
                name = "Threadpool max",
                description = "the size (maximum) of the thread pool for clientlib processing (must be equal or greater than the minimum)"
        )
        int clientlibs_threadpool_max() default 20;

        @AttributeDefinition(
                name = "Rerender On Nocache",
                description = "Renders clientlib again if a no-cache header is received - mainly debugg the clientlib mechanism itself. Changes in the JS / CSS files automatically lead to re-rendering the clientlib."
        )
        boolean rerender_on_nocache() default false;

    }

    Config getConfig();
}
