package com.composum.sling.nodes.components.codeeditor;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.nodes.browser.Browser;
import com.composum.sling.nodes.console.ConsolePage;
import com.composum.sling.nodes.console.ConsoleServletBean;
import org.apache.sling.api.resource.Resource;
import org.apache.tika.mime.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.composum.sling.nodes.browser.Browser.EDITOR_MODES;

public class CodeEditor extends ConsoleServletBean {

    private static final Logger LOG = LoggerFactory.getLogger(ConsolePage.class);

    private transient String textType;

    public CodeEditor(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public CodeEditor(BeanContext context) {
        super(context);
    }

    public CodeEditor() {
        super();
    }

    public String getContentPath() {
        ResourceHandle content = resource.getContentResource();
        return content.isValid() ? content.getPath() : getPath();
    }

    public String getTextType() {
        if (textType == null) {
            MimeType mimeType = MimeTypeUtil.getMimeType(resource);
            String extension = ResourceUtil.getNameExtension(resource);
            textType = Browser.getFileType(EDITOR_MODES, mimeType != null ? mimeType.toString() : "", extension);
        }
        return textType;
    }
}
