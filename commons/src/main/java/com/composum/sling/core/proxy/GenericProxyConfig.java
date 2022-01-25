package com.composum.sling.core.proxy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * a generic proxy request service configuration object
 */
@ObjectClassDefinition(
        name = "Composum Generic Proxy Request"
)
public @interface GenericProxyConfig {

    @AttributeDefinition(
            description = "the general on/off switch for this service"
    )
    boolean enabled() default true;

    @AttributeDefinition(
            description = "the name (short descriptive key) of the service"
    )
    String name();

    @AttributeDefinition(
            description = "the pattern of the target URL (proxy suffix) handled by the proxy service"
    )
    String targetPattern();

    @AttributeDefinition(
            description = "the URL for the proxy request (optional; if different from the target pattern)"
    )
    String targetUrl();

    @AttributeDefinition(
            description = "a comma separated list of tags to change to anther tag name (old:new[ static=\"attribute\"])"
    )
    String[] tags_to_rename() default {"html:div"};

    @AttributeDefinition(
            description = "a comma separated list of tags to strip from the result (this keeps the tags body)"
    )
    String[] tags_to_strip() default {"body"};

    @AttributeDefinition(
            description = "a comma separated list of tags to drop from the result (this removes the tags body)"
    )
    String[] tags_to_drop() default {"head", "style", "script", "link"};

    @AttributeDefinition(
            description = "a XSLT filter chain (set of XSLT file resource paths) to transform the content"
    )
    String[] XSLT_chain_paths();

    @AttributeDefinition(
            description = "the repository path which repesents this service (for ACL based permission check)"
    )
    String referencePath();

    @AttributeDefinition()
    String webconsole_configurationFactory_nameHint() default
            "{name} (enabled: {enabled}, target: {targetPattern}, url: {targetUrl}, ref: {referencePath})";
}
