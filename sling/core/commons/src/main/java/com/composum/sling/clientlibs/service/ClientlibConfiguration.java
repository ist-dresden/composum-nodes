package com.composum.sling.clientlibs.service;

public interface ClientlibConfiguration {

    boolean getDebug();

    // CSS configuration

    boolean getCssMinimize();

    int getCssLineBreak();

    String getCssTemplate();

    // JS configuration

    boolean getJavascriptMinimize();

    boolean getJavascriptMunge();

    boolean getJavascriptOptimize();

    int getJavascriptLineBreak();

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
}
