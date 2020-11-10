<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="group" type="com.composum.sling.core.usermanagement.view.Group" scope="request">
    <div class="members detail-panel full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="add-authorizable-to-group fa fa-plus btn btn-default" title="Add authorizable to group"><span class="label">Add authorizable to group</span></button>
                <button class="remove-authorizable-from-group fa fa-minus btn btn-default" title="Remove authorizable from group"><span class="label">Remove authorizable from group</span></button>
            </div>
        </div>
        <div class="table-container">
            <table id="group-view-members-table" class="members-table"
                   data-path="${group.suffix}"
                   data-toolbar=".members .table-toolbar">
            </table>
        </div>
    </div>
</cpn:component>