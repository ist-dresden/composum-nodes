<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="user" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="user detail-panel full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="disable-user fa fa-ban btn btn-default" title="Disable"><span class="label">Disable user</span></button>
                <button class="enable-user fa fa-check-circle-o btn btn-default" title="Enable"><span class="label">Enable user</span></button>
                <button class="change-password fa fa-key btn btn-default" title="Change Password"><span class="label">Change Password</span></button>
            </div>
        </div>
        <div class="table-container">
            <table id="user-view-user-table" class="user-table"
                   data-path="${user.suffix}"
                   data-toolbar=".user .table-toolbar">
            </table>
        </div>
    </div>
</cpn:component>