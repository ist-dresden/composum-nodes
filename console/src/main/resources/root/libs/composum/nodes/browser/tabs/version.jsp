<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <c:set var="writeAllowed" value="${browser.permissible['nodes/repository/versions']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="versions detail-panel full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="checkpoint fa fa-circle-o btn btn-default" title="Checkpoint"${writeDisabled}><span
                        class="label">Create Checkpoint</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="checkout fa fa-sign-out btn btn-default" title="Checkout"${writeDisabled}><span
                        class="label">Checkout Node</span></button>
                <button class="checkin fa fa-sign-in btn btn-default" title="Checkin"${writeDisabled}><span
                        class="label">Checkin Node</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="restore fa fa-undo btn btn-default"
                        title="Rollback to a specific version, deleting later versions."${writeDisabled}><span
                        class="label">Rollback Version</span>
                </button>
                <button class="delete fa fa-minus btn btn-default" title="Delete a Version"${writeDisabled}><span
                        class="label">Delete Version</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="add fa fa-tags btn btn-default" title="Add new label"${writeDisabled}><span
                        class="label">Add Label</span></button>
                <button class="remove fa fa-tags btn btn-default" title="Remove a label"${writeDisabled}><span
                        class="label">Remove Label </span></button>
            </div>
        </div>
        <div class="table-container">
            <table id="browser-view-version-table" class="version-table"
                   data-path="${browser.current.pathEncoded}"
                   data-toolbar=".node-view-panel .versions .table-toolbar">
            </table>
        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
