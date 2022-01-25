package com.composum.sling.cpnl;

import com.composum.sling.core.util.LinkMapper;
import com.composum.sling.core.util.LinkUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * a tag to build image elements with mapped source URLs
 */
public class ImageTag extends UrlTag {

    protected String title;
    protected String alt;

    @Override
    protected void clear() {
        super.clear();
        alt = null;
        title = null;
    }

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

    public void setTitle(String value) {
        this.title = value;
    }

    public void setAlt(String value) {
        this.alt = value;
    }

    @Override
    @NotNull
    protected String buildUrl(@NotNull String urlValue, @Nullable final Boolean map) {
        if (map != null) {
            urlValue = LinkUtil.getUrl(request, urlValue, null, "",
                    map ? LinkMapper.RESOLVER : LinkMapper.CONTEXT);
        } else {
            urlValue = LinkUtil.getUrl(request, urlValue, "");
        }
        return urlValue;
    }

    @Override
    protected void writeAttributes(JspWriter writer) throws IOException {
        super.writeAttributes(writer);
        if (StringUtils.isNotBlank(title)) {
            writer.write(" title=\"");
            writer.write(CpnlElFunctions.text(title));
            writer.write("\"");
        }
        if (StringUtils.isNotBlank(alt)) {
            writer.write(" alt=\"");
            writer.write(CpnlElFunctions.text(alt));
            writer.write("\"");
        }
    }
}
