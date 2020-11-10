<%@page session="false" pageEncoding="utf-8"
        import="org.apache.sling.api.resource.ValueMap" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<%
    String redirect = (String) resource.adaptTo(ValueMap.class).get("sling:redirect");
    if (redirect != null && (redirect = redirect.trim()).length() > 0) {
        response.sendRedirect(redirect);
    } else {
%>
<html data-context-path="${slingRequest.contextPath}">
<sling:call script="head.jsp"/>
<cpn:clientlib type="css" path="composum/nodes/console/clientlibs/page"/>
<sling:call script="body.jsp"/>
</html>
<%
    }
%>
