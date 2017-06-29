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

}
