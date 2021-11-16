<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="user" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <c:set var="writeAllowed" value="${user.permissible['nodes/users/manager']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="groups detail-tab full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="add-authorizable-to-group fa fa-plus btn btn-default"${writeDisabled}
                        title="${cpn:i18n(slingRequest,'Add user to group')}"><span
                        class="label">${cpn:i18n(slingRequest,'Add user to group')}</span></button>
                <button class="remove-authorizable-from-group fa fa-minus btn btn-default"${writeDisabled}
                        title="${cpn:i18n(slingRequest,'Remove user from group')}"><span
                        class="label">${cpn:i18n(slingRequest,'Remove user from group')}</span></button>
            </div>
        </div>
        <div class="table-container">
            <table id="user-view-groups-table" class="groups-table"
                   data-path="${user.path}"
                   data-toolbar=".groups .table-toolbar">
            </table>
        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>