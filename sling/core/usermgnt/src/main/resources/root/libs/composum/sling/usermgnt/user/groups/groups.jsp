<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="user" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="groups detail-panel full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                    <button class="add-authorizable-to-group fa fa-plus btn btn-default" title="Add user to group"><span class="label">Add user to group</span></button>
                    <button class="remove-authorizable-from-group fa fa-minus btn btn-default" title="Remove user from group"><span class="label">Remove user from group</span></button>
            </div>
        </div>
        <div class="table-container">
            <table id="user-view-groups-table" class="groups-table"
                   data-path="${user.suffix}"
                   data-toolbar=".groups .table-toolbar">
            </table>
        </div>
    </div>
</cpn:component>