<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
    <div class="detail-panel filters ${pckg.cssClasses} full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="edit fa fa-pencil btn btn-default" title="Edit filter rules"><span class="label">Edit</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="add fa fa-plus btn btn-default" title="Add new filter"><span class="label">Add</span></button>
                <button class="remove fa fa-minus btn btn-default" title="Remove selected filters"><span class="label">Remove</span></button>
            </div>
            <!-- div class="btn-group btn-group-sm" role="group">
                <button class="copy fa fa-copy btn btn-default" title="Copy selected filter"><span class="label">Copy</span></button>
                <button class="paste fa fa-paste btn btn-default" title="Paste copied filter"><span class="label">Paste</span></button>
            </div -->
            <div class="btn-group btn-group-sm" role="group">
                <button class="move-up fa fa-arrow-up btn btn-default" title="Move filter up"><span class="label">Up</span></button>
                <button class="move-down fa fa-arrow-down btn btn-default" title="Move filter down"><span class="label">Down</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
            </div>
        </div>
        <div class="table-container">
            <cpn:table classes="filters-table"
                       path="/bin/cpm/package.filterList.json${pckg.path}"
                       toolbar=".detail-content .detail-panel.filters .table-toolbar">
            </cpn:table>
        </div>
    </div>
</cpn:component>