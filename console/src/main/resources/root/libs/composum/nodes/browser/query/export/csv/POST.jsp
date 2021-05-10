<%@page session="false" pageEncoding="utf-8"
        import="org.apache.commons.lang3.StringUtils,
                org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                java.io.PrintWriter" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %><%
%><sling:defineObjects/><%
    ValueMap values = resource.getValueMap();
    String filename = values.get("filename", String.class);
    if (StringUtils.isBlank(filename)) {
        filename = "query-export.csv";
    } else {
        while (filename.startsWith("/")) {
            filename = filename.substring(1);
        }
    }
    slingResponse.setContentType("text/comma-separated-values; charset=UTF-8");
    slingResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    String[] propertySet = values.get("properties", new String[]{
            "name;Name",
            "path;Path",
            "jcr:title;Title",
            "jcr:primaryType;Primary Type",
            "sling:resourceType;Resource Type"
    });
    PrintWriter writer = slingResponse.getWriter();
    for (int i = 0; i < propertySet.length; i++) {
        String[] keyLabel = StringUtils.split(propertySet[i], ";", 2);
        if (i > 0) {
            writer.print(',');
        }
        writer.append("\"").append((keyLabel.length < 2 ? keyLabel[0] : keyLabel[1]).replaceAll("\"", "\"\"")).append("\"");
    }
    writer.println();
    for (Resource item : resource.getChildren()) {
        ValueMap valueMap = item.getValueMap();
        for (int i = 0; i < propertySet.length; i++) {
            String[] keyLabel = StringUtils.split(propertySet[i], ";", 2);
            if (i > 0) {
                writer.print(',');
            }
            Object value;
            switch (keyLabel[0]) {
                case "name":
                    value = item.getName();
                    break;
                case "path":
                    value = item.getPath();
                    break;
                default:
                    value = valueMap.get(keyLabel[0]);
                    break;
            }
            writer.append("\"").append(value != null ? value.toString().replaceAll("\"", "\"\"") : "").append("\"");
        }
        writer.println();
    }
%>