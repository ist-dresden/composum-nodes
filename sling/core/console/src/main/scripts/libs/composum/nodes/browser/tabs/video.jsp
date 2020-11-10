<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="video detail-panel" data-type="${browser.current.mimeType}" data-path="${browser.current.pathUrl}"
         data-mapped="${browser.current.url}">
        <div class="video-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <span class="resolver fa fa-external-link btn btn-default" title="Resolver Mapping ON/OFF"></span>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <div class="parameters input-group input-group-sm">
                    <span class="fa fa-question input-group-addon" title="URL parameters"></span>
                    <input type="text" class="form-control" placeholder="URL parameters">
                </div>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
                <button type="button" class="open fa fa-globe btn btn-default" title="Open in a separate view"><span
                        class="label">Open</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <a href="" class="download fa fa-download btn btn-default" title="Download video"
                   target="_blank"><span class="label">Download</span></a>
                <button type="button" class="update fa fa-upload btn btn-default" title="Change Video"></button>
            </div>
        </div>
        <div class="detail-content">
            <div class="video-frame">
                <div class="video-background">
                    <video class="video-player" controls>
                        <source type="" src=""/>
                    </video>
                </div>
            </div>
        </div>
    </div>
</cpn:component>
