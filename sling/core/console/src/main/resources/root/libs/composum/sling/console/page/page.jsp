<%@page session="false" pageEncoding="utf-8"
    import="org.apache.sling.api.resource.ValueMap" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects/>
<%
    String redirect = (String) resource.adaptTo(ValueMap.class).get("sling:redirect");
    if (redirect != null && (redirect = redirect.trim()).length() > 0) {
        response.sendRedirect(redirect);
    } else {
%>
<sling:call script="overview.jsp"/>
<%
    }
%>
