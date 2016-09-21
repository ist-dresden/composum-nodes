package com.composum.sling.clientlibs.service;

public interface ClientlibConfiguration {

    // CSS configuration

    boolean getCssDebug();

    boolean getCssMinimize();

    int getCssLineBreak();

    String getCssTemplate();

    // JS configuration

    boolean getJavascriptDebug();

    boolean getJavascriptMinimize();

    boolean getJavascriptMunge();

    boolean getJavascriptOptimize();

    int getJavascriptLineBreak();

    String getJavascriptTemplate();

    // Link configuration

    String getLinkTemplate();

    // general configuration

    boolean getMapClientlibURLs();

    boolean getGzipEnabled();

    String getCacheRoot();

    int getThreadPoolMin();

    int getThreadPoolMax();
}
