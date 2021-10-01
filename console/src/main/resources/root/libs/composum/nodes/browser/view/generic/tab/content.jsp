<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="genericView" type="com.composum.sling.nodes.browser.GenericView" scope="request">
    <div class="detail-content">
        <sling:include resourceType="${genericView.content.resourceType}"/>
    </div>
</cpn:component>