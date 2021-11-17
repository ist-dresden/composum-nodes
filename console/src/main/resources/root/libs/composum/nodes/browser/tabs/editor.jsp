<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <c:set var="writeAllowed" value="${browser.permissible['nodes/repository/source']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
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
                    <button type="button" class="start-editing fa fa-pencil btn btn-default"
                            title="Edit text"${writeDisabled}><span class="label">Edit</span></button>
                    <a href="${writeAllowed?browser.editCodeUrl:'#'}" target="${browser.current.path}"
                       class="edit-window fa fa-edit btn btn-default${writeAllowed?'':' disabled'}"
                       title="Edit text in a new window (browser tab)"><span class="label">Edit Window</span></a>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <a href="" class="download fa fa-download btn btn-default" title="Download text file"><span
                            class="label">Download</span></a>
                    <button type="button" class="update fa fa-upload btn btn-default"
                            title="Update text file"${writeDisabled}><span class="label">Update File</span></button>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                    </button>
                    <c:if test="${browser.renderable}">
                        <cpn:link href="${browser.filePath}" class="fa fa-globe btn btn-default"
                                  title="Open in a separate view" target="_blank"><span
                                class="label">Open</span></cpn:link>
                    </c:if>
                </div>
            </div>
            <div class="editor-frame detail-content">
                <div class="code-editor" data-path="${browser.contentResource.path}" data-file="${browser.filePath}"
                     data-type="${browser.textType}">
                </div>
            </div>
        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>