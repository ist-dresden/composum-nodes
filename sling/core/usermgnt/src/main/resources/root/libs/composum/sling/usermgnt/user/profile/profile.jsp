<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="user" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="profile detail-panel full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
            </div>
        </div>
        <div class="table-container">
            <table id="user-view-user-table" class="profile-table"
                   data-path="${user.suffix}"
                   data-toolbar=".user .table-toolbar">
            </table>
        </div>
    </div>
</cpn:component>