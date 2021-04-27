<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.ServiceUser" scope="request">
    <div class="paths detail-tab">
        <div class="paths-toolbar detail-toolbar">
        </div>
        <div class="paths-container">
            <sling:include resourceType="composum/nodes/usermgnt/graph/view" replaceSelectors="paths"/>
        </div>
    </div>
</cpn:component>
