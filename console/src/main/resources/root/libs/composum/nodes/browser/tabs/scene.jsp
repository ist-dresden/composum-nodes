<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
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
                <button type="button" class="prepare fa fa-window-maximize btn btn-default" title="Prepare"><span class="label">Reload</span>
                </button>
                <button type="button" class="remove fa fa-trash btn btn-default" title="Remove"><span class="label">Reload</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
            </div>
        </div>
        <div class="embedded frame-container detail-content">
            <iframe src="" width="100%" height="100%"></iframe>
        </div>
    </div>
</cpn:component>