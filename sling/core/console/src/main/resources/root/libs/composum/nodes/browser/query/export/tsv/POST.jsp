<%@page session="false" pageEncoding="utf-8"
        import="org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                java.io.PrintWriter,
                com.composum.sling.core.util.XSS" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %><%
%><sling:defineObjects/><%
    String filename = XSS.filter(slingRequest.getRequestPathInfo().getSuffix());
    if (filename == null) {
        filename = "query-export.tsv";
    } else {
        while (filename.startsWith("/")) {
            filename = filename.substring(1);
        }
    }
    slingResponse.setContentType("text/tab-separated-values; charset=UTF-8");
    slingResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    PrintWriter writer = slingResponse.getWriter();
    writer.println("Name\tPath\tTitle\tPrimary Type\tResource Type");
    for (Resource item : resource.getChildren()) {
        ValueMap valueMap = item.adaptTo(ValueMap.class);
        writer.append(item.getName().replace('\t', ' ')).append("\t");
        writer.append(item.getPath().replace('\t', ' ')).append("\t");
        writer.append(valueMap.get("title", valueMap.get("jcr:title", "")).replace('\t', ' ')).append("\t");
        writer.append(valueMap.get("jcr:primaryType", "")).append("\t");
        writer.append(valueMap.get("sling:resourceType", ""));
        writer.println();
    }
%>