package com.composum.sling.cpnl;

/**
 * a tag to build image elements with mapped source URLs
 */
public class ImageTag extends UrlTag {

    @Override
    protected String getDefaultTagName() {
        return "img";
    }

    @Override
    protected String getDefaultUrlAttr() {
        return "src";
    }

    public void setSrc(String src) {
        setUrl(src);
    }
}
