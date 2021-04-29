<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="profile" type="com.composum.sling.core.user.UserProfile" scope="request">
    <cpn:div class="profile-name name property" test="${not empty profile.name}">
        <div class="label">${cpn:i18n(slingRequest,'Name')}</div>
        <cpn:text class="value">${profile.name}</cpn:text>
    </cpn:div>
    <sling:call script="properties.jsp"/>
</cpn:component>
