<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="node-view-panel detail-panel text">
        <a class="favorite-toggle fa fa-star-o" href="#" title="Tooggle favorite state"><span
                class="label">Favorite</span></a>
        <div class="node-tabs detail-tabs action-bar btn-toolbar" role="toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <a class="properties fa fa-list btn btn-default" href="#properties" data-group="properties"
                   title="Node Properties"><span class="label">Properties</span></a>
                <c:if test="${browser.renderable}">
                    <a class="view fa fa-eye btn btn-default" href="#display" data-group="view"
                       title="Display Rendered View"><span class="label">View</span></a>
                </c:if>
                <a class="code fa fa-file-text-o btn btn-default" href="#editor"
                   data-group="${browser.renderable?'edit':'view'}" title="Text/Code View"><span
                        class="label">Text/Code</span></a>
                <sling:call script="/libs/composum/nodes/browser/views/std/tabs.jsp"/>
            </div>
        </div>
        <div class="node-view-content detail-content">
        </div>
    </div>
</cpn:component>
