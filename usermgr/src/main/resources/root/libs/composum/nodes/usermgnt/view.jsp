<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.View" scope="request">
    <div class="detail-view">
        <sling:include resourceType="composum/nodes/usermgnt/${model.viewType}"/>
    </div>
</cpn:component>
