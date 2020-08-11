<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.platform.commons.request.ErrorPage">
<%
    if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ||
            !model.redirectToLogin(slingRequest, slingResponse)) {
        // send the raw status code in the case of an Ajax request of ig the login request itself is not allowed
        slingResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
%>
</cpn:component>
