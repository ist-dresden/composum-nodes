<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.view.PackageManagerBean" scope="request">
    <html data-context-path="${slingRequest.contextPath}">
    <sling:call script="head.jsp"/>
    <body id="pckgmgr" class="console left-open top-open">
    <div id="ui">
        <sling:call script="dialogs.jsp"/>
        <sling:include resourceType="composum/nodes/console/components/navbar"/>
        <div id="content-wrapper">
            <div id="split-view-horizontal-split" class="split-pane horizontal-split fixed-left">
                <div class="split-pane-component left-pane">
                    <div>
                        <div class="tree-panel">
                            <div id="package-tree" data-selected="${pckgmgr.path}">
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
                </div>
                <div class="split-pane-divider"></div>
                <div class="split-pane-component right-pane">
                    <div id="split-view-vertical-split" class="split-pane vertical-split fixed-top">
                        <div class="split-pane-component top-pane">
                            <div id="pckgmgr-query">
                                <sling:call script="query.jsp"/>
                            </div>
                        </div>
                        <div class="split-pane-divider"></div>
                        <div class="split-pane-component bottom-pane">
                            <div id="pckgmgr-view">
                                <sling:call script="view.jsp"/>
                            </div>
                            <div class="close-top"><a href="#" class="fa fa-angle-double-up"
                                                      title="Collapse top panel"></a></div>
                        </div>
                        <div class="open-top"><a href="#" class="fa fa-angle-double-down" title="Restore top panel"></a>
                        </div>
                    </div>
                    <div class="close-left"><a href="#" class="fa fa-angle-double-left" title="Collapse left panel"></a>
                    </div>
                </div>
                <div class="open-left"><a href="#" class="fa fa-angle-double-right" title="Restore left panel"></a>
                </div>
            </div>
        </div>
    </div>
    <sling:call script="script.jsp"/>
    <sling:include resourceType="composum/nodes/console/components/tryLogin"/>
    </body>
    </html>
</cpn:component>
