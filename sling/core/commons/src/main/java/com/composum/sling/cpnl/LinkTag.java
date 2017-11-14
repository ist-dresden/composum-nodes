package com.composum.sling.cpnl;

/**
 * a tag to build hypertext links with mapped URLs
 */
public class LinkTag extends UrlTag {

    protected String getDefaultTagName() {
        return "a";
    }

    protected String getDefaultUrlAttr() {
        return "href";
    }

    public void setHref(String href) {
        setUrl(href);
    }
}
