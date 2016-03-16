<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="group" type="com.composum.sling.core.usermanagement.view.Group" scope="request">
    <div class="group detail-panel full-table-view">
        <div class="table-toolbar">
        </div>
        <div class="table-container">
            <table id="user-view-user-table" class="user-table"
                   data-path="${group.suffix}"
                   data-toolbar=".group .table-toolbar">
            </table>
        </div>
    </div>
</cpn:component>