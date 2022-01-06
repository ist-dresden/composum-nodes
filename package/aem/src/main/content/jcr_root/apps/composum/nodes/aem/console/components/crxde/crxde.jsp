<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects/>
<%
    String suffix = slingRequest.getRequestPathInfo().getSuffix();
    response.sendRedirect(request.getContextPath() + "/crx/de/index.jsp" + (suffix != null ? ("#" + suffix) : ""));
%>
