<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects />
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.View" scope="request">
    <sling:include resourceType="composum/nodes/usermgnt/${model.viewType}/${model.tabType}" />
</cpn:component>
