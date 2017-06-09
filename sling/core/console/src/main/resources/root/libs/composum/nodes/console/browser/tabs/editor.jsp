<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="editor detail-panel">
        <div class="widget code-editor-widget">
            <div class="editor-toolbar detail-toolbar">
                <div class="btn-group btn-group-sm" role="group">
                    <div class="search input-group input-group-sm">
                        <input type="text" class="find-text form-control" placeholder="search in text">
                        <span class="find-prev fa fa-chevron-left input-group-addon" title="find previous"></span>
                        <span class="find-next fa fa-chevron-right input-group-addon" title="find next"></span>
                    </div>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="start-editing fa fa-pencil btn btn-default" title="Edit text"><span
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
            <div class="editor-frame detail-content">
                <div class="code-editor" data-path="${browser.contentResource.path}" data-type="${browser.textType}">
                </div>
            </div>
        </div>
    </div>
</cpn:component>