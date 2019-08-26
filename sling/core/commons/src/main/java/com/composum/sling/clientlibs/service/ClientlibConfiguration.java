package com.composum.sling.clientlibs.service;

public interface ClientlibConfiguration {

    boolean getDebug();

    // CSS configuration

    boolean getCssMinimize();

    int getCssLineBreak();

    String getCssTemplate();

    // JS configuration

    String getJavascriptTemplate();

    // Link configuration

    String getLinkTemplate();

    // general configuration

    boolean getMapClientlibURLs();

    boolean getUseMinifiedFiles();

    boolean getGzipEnabled();

    String getCacheRoot();

    int getThreadPoolMin();

    int getThreadPoolMax();

    /** General - Resolver cachetime : the time (in seconds) the clientlib resolver caches the locations of all client libraries for a category. <=0 means no caching. */
    int getResolverCachetime();

    /** Inserts HTML comments with the client libraries that have been called up into the page */
    boolean getTagDebug();

    /** Renders clientlib again if a no-cache header is received - mainly for debugging purposes. Changes in the JS / CSS files automatically lead to re-rendering the clientlib. */
    boolean getRerenderOnNocache();

}
