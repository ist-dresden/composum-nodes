<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="genericView" type="com.composum.sling.nodes.browser.GenericView" scope="request">
    <sling:include resourceType="${genericView.viewResourceType}"/>
</cpn:component>
