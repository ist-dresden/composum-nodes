<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpn:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="node-view-panel detail-panel video">
        <a class="favorite-toggle fa fa-star-o" href="#" title="Tooggle favorite state"><span
                class="label">Favorite</span></a>
        <div class="node-tabs detail-tabs action-bar btn-toolbar" role="toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <a class="properties fa fa-list btn btn-default" href="#properties" data-group="properties"
                   title="Node Properties"><span class="label">Properties</span></a>
                <a class="view fa fa-eye btn btn-default" href="#video" data-group="view" title="Watch video"><span
                        class="label">View</span></a>
                <sling:call script="${composumBase}composum/nodes/browser/views/std/tabs.jsp"/>
            </div>
        </div>
        <div class="node-view-content detail-content">
        </div>
    </div>
</cpn:component>
