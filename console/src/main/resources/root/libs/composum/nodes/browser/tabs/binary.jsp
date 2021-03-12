<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="binary detail-panel" data-file="${cpn:path(browser.filePath)}">
        <div class="image-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <a href="" class="download fa fa-download btn btn-default" title="Download File"
                   target="_blank"><span class="label">Download</span></a>
                <button type="button" class="update fa fa-upload btn btn-default" title="Change Content"></button>
            </div>
        </div>
        <div class="frame-container detail-content">
            <sling:call script="tabs/binary-view.jsp"/>
        </div>
    </div>
</cpn:component>