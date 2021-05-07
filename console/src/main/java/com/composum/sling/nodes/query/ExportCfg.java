package com.composum.sling.nodes.query;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.XSS;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import java.nio.charset.StandardCharsets;

public class ExportCfg extends ConfigItem {

    public static final String PROP_FILENAME = "filename";
    public static final String PROP_SELECTORS = "selectors";
    public static final String PROP_EXPORT_TYPE = "exportType";
    public static final String PROP_QUERY = "query";
    public static final String PROP_FILTER = "filter";
    public static final String PROP_PROPERTIES = "properties";

    public ExportCfg(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public ExportCfg(BeanContext context) {
        super(context);
    }

    public ExportCfg() {
        super();
    }

    public String getFilename() {
        return getProperty(PROP_FILENAME, RequestUtil.getParameter(getRequest(), PROP_FILENAME, ""));
    }

    public String getSelectors() {
        String selectors = getProperty(PROP_SELECTORS, RequestUtil.getParameter(getRequest(), PROP_SELECTORS, ""));
        if (StringUtils.isNotBlank(selectors) && !selectors.startsWith(".")) {
            selectors = "." + selectors;
        }
        return selectors;
    }

    public String getExportType() {
        return getProperty(PROP_EXPORT_TYPE, "");
    }

    public String getQuery() {
        return Base64.encodeBase64String(XSS.filter(request.getParameter(PROP_QUERY)).getBytes(StandardCharsets.UTF_8));
    }

    public String getFilter() {
        return getProperty(PROP_FILTER, RequestUtil.getParameter(getRequest(), PROP_FILTER, ""));
    }

    public String getProperties() {
        String[] properties = getProperty(PROP_PROPERTIES, String[].class);
        if (properties == null) {
            String propertySet = XSS.filter(request.getParameter(PROP_PROPERTIES));
            if (StringUtils.isNotBlank(propertySet)) {
                properties = StringUtils.split(propertySet, ',');
            }
        }
        return properties != null ? StringUtils.join(properties, ',') : "";
    }
}
