<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="script detail-panel">
        <div class="widget code-editor-widget">
            <div class="editor-toolbar detail-toolbar">
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="run-script btn btn-default btn-stack" title="Run Script"><span
                            class="fa-stack"><i class="fa fa-spin fa-gear fa-stack-2x background-text"></i><i
                            class="symbol fa fa-play fa-stack-1x"></i><i
                            class="running fa fa-stop fa-stack-1x"></i><i
                            class="error fa fa-stack-2x">!</i></span><span
                            class="label">Run Script</span></button>
                    <button type="button" class="history fa fa-history btn btn-default" title="Toggle History"><span
                            class="label">History</span></button>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <div class="search input-group input-group-sm">
                        <input type="text" class="find-text form-control" placeholder="search in text">
                        <span class="find-prev fa fa-chevron-left input-group-addon" title="find previous"></span>
                        <span class="find-next fa fa-chevron-right input-group-addon" title="find next"></span>
                    </div>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="start-editing fa fa-pencil btn btn-default" title="Edit Script"><span
                            class="label">Edit</span></button>
                    <a href="${browser.editCodeUrl}" target="${browser.current.path}"
                       class="edit-window fa fa-edit btn btn-default"
                       title="Edit text in a new window (browser tab)"><span class="label">Edit Window</span></a>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <a href="" class="download fa fa-download btn btn-default" title="Download text file"
                       target="_blank"><span class="label">Download</span></a>
                    <button type="button" class="update fa fa-upload btn btn-default" title="Update text file"><span
                            class="label">Update File</span></button>
                </div>
            </div>
            <div class="detail-content">
                <div id="script-view" class="split-pane vertical-split fixed-bottom">
                    <div class="split-pane-component top-pane">
                        <div class="editor-frame">
                            <div class="code-editor" data-path="${browser.contentResource.path}"
                                 data-type="${browser.textType}">
                            </div>
                        </div>
                    </div>
                    <div class="split-pane-divider"></div>
                    <div class="split-pane-component bottom-pane">
                        <div class="bottom-area">
                            <div class="log-area">
                                <div class="log-output">log output...</div>
                            </div>
                            <div class="history">
                                <div class="action-bar btn-toolbar toolbar">
                                    <div class="btn-group btn-group-sm" role="group">
                                        <button type="button" class="close fa fa-close btn btn-default"
                                                title="Close Audit Log"><span
                                                class="label">Close</span></button>
                                    </div>
                                    <div class="btn-group btn-group-sm" role="group">
                                        <a class="audit-link" href="#" data-path="${browser.path}"><label>Audit
                                            Log</label></a>
                                    </div>
                                    <div class="btn-group btn-group-sm align-right" role="group">
                                        <button type="button" class="refresh fa fa-refresh btn btn-default"
                                                title="Refresh Audit Log"><span
                                                class="label">Refresh</span></button>
                                        <button type="button" class="purge fa fa-trash-o btn btn-default"
                                                title="Purge Audit Log"><span
                                                class="label">Purge</span></button>
                                    </div>
                                </div>
                                <ul class="executions">
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</cpn:component>