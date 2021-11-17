<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <c:set var="writeAllowed" value="${browser.permissible['nodes/components/scenes']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="scene detail-panel" data-path="${browser.currentPathUrl}">
        <div class="scene-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <select class="scene-key widget select-widget form-control">
                    <c:forEach items="${browser.availableScenes}" var="scene">
                        <c:forEach items="${scene.tools}" var="tool">
                            <option value="${cpn:value(scene.key)}/${cpn:value(tool.name)}"
                                    title="${cpn:text(tool.description)}">${cpn:text(tool.label)}</option>
                        </c:forEach>
                    </c:forEach>
                </select>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="content fa fa-share btn btn-default"
                        title="Jump to the scenes content resource"><span
                        class="label">Content</span>
                </button>
                <button type="button" class="prepare fa fa-asterisk btn btn-default"
                        title="Prepare or reset the scenes content"><span
                        class="label"${writeDisabled}>Prepare</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="remove fa fa-trash btn btn-default" title="Remove the scenes content"><span
                        class="label"${writeDisabled}>Remove</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload the current scene">
                    <span class="label">Reload</span>
                </button>
                <button type="button" class="open fa fa-globe btn btn-default" title="Open in a separate view"><span
                        class="label">Open</span></button>
            </div>
        </div>
        <div class="embedded frame-container detail-content">
            <iframe src="" width="100%" height="100%"
                    sandbox="allow-same-origin allow-scripts allow-modals allow-popups allow-forms allow-downloads"></iframe>
        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>