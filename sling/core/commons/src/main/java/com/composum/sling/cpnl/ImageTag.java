package com.composum.sling.cpnl;

/**
 * a tag to build image elements with mapped source URLs
 */
public class ImageTag extends UrlTag {

    protected String getDefaultTagName() {
        return "img";
    }

    protected String getDefaultUrlAttr() {
        return "src";
    }

    public void setSrc(String src) {
        setUrl(src);
    }
}
