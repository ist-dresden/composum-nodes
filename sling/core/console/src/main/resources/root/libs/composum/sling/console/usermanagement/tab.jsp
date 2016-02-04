<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects />
<cpn:component id="usermanagement" type="com.composum.sling.core.usermanagement.UserManagement" scope="request">
    <sling:include resourceType="composum/sling/console/usermanagement/${usermanagement.viewType}/${usermanagement.tabType}" />
</cpn:component>