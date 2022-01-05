<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="node-view-panel detail-panel binary">
        <a class="favorite-toggle fa fa-star-o" href="#" title="Tooggle favorite state"><span
                class="label">Favorite</span></a>
        <div class="node-tabs detail-tabs action-bar btn-toolbar" role="toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <a class="properties fa fa-list btn btn-default" href="#properties" data-group="properties"
                   title="Node Properties"><span class="label">Properties</span></a>
                <a class="download fa fa-${browser.fileIcon} btn btn-default" href="#binary" data-group="view"
                   title="Download Binary Asset"><span class="label">Download</span></a>
                <sling:call script="/libs/composum/nodes/browser/views/std/tabs.jsp"/>
            </div>
        </div>
        <div class="node-view-content detail-content">
        </div>
    </div>
</cpn:component>
