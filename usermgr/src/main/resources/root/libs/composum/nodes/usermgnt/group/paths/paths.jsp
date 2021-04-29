<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="user" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="paths detail-tab">
        <div class="paths-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default"
                        title="${cpn:i18n(slingRequest,'Reload')}"><span
                        class="label">${cpn:i18n(slingRequest,'Reload')}</span>
                </button>
            </div>
        </div>
        <div class="paths-container">
            <sling:include resourceType="composum/nodes/usermgnt/graph/view" replaceSelectors="paths"/>
        </div>
    </div>
</cpn:component>
