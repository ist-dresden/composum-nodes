<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.manager.view.PackageManagerBean" scope="request">
    <div class="nodes-pckgmgr-jcrpckg-tree">
        <div class="tree-panel">
            <div id="jcrpckg-tree" data-selected="${pckgmgr.path}">
            </div>
        </div>
        <div class="tree-actions action-bar btn-toolbar" role="toolbar">
            <div class="align-left">
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="refresh glyphicon-refresh glyphicon btn btn-default"
                            title="Refresh tree view"><span class="label">Refresh</span></button>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="create fa fa-plus btn btn-default"
                            title="Create a new package"><span class="label">Create</span></button>
                    <button type="button" class="delete fa fa-minus btn btn-default"
                            title="Delete selected package"><span class="label">Delete</span></button>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="upload fa fa-upload btn btn-default"
                            title="Upload a new package"><span class="label">Upload</span></button>
                    <a type="button" class="download fa fa-download btn btn-default"
                       title="Download selected package"><span class="label">Download</span></a>
                </div>
            </div>
        </div>
    </div>
</cpn:component>
