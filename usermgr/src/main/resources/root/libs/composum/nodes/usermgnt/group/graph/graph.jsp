<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="user" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="graph detail-tab">
        <div class="graph-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="toggle-image fa fa-image btn btn-default"
                        title="${cpn:i18n(slingRequest,'Toggle SVG Image view')}"><span
                        class="label">${cpn:i18n(slingRequest,'Toggle SVG Image view')}</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="open-graph-page fa fa-external-link btn btn-default"
                        title="${cpn:i18n(slingRequest,'Open Authorizable Graph page')}"><span
                        class="label">${cpn:i18n(slingRequest,'Open Authorizable Graph page')}</span>
                </button>
            </div>
        </div>
        <div class="graph-container">
            <sling:include resourceType="composum/nodes/usermgnt/graph/view"/>
        </div>
    </div>
</cpn:component>
