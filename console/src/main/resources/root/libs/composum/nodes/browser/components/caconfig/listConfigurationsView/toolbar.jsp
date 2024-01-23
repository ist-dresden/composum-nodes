<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model"
               type="com.composum.sling.nodes.components.CAConfigModel"
               scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
    <c:set var="writeAllowed" value="${model.permissible['nodes/repository/permissions']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>

    <div class="caconfig-toolbar detail-toolbar flex-toolbar">
        <div class="detail-headline" style="margin-right: auto; font-weight: bold;">Configuration Types</div>
        <div class="btn-group btn-group-sm" role="group">
            <button type="button" class="refresh fa fa-refresh btn btn-default"
                    title="${cpn:i18n(slingRequest,'Reload')}"><span
                    class="label">${cpn:i18n(slingRequest,'Reload')}</span>
            </button>
        </div>
    </div>

    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
