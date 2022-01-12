<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <c:set var="writeAllowed" value="${browser.permissible['nodes/repository/resources']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="image detail-panel" data-file="${cpn:path(browser.filePath)}"
         data-path="${browser.currentPathUrl}" data-mapped="${browser.currentUrl}">
        <div class="image-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <span class="resolver fa fa-external-link btn btn-default" title="Resolver Mapping ON/OFF"></span>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <div class="selectors input-group input-group-sm">
                    <span class="input-group-addon" title="Sling selectors">.x.</span>
                    <input type="text" class="form-control" placeholder="Sling selectors">
                </div>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <div class="parameters input-group input-group-sm">
                    <span class="fa fa-question input-group-addon" title="URL parameters"></span>
                    <input type="text" class="form-control" placeholder="URL parameters">
                </div>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <a href="" class="download fa fa-download btn btn-default" title="Download image"><span class="label">Download</span></a>
                <button type="button" class="update fa fa-upload btn btn-default"
                        title="Change Image"${writeDisabled}></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
                <cpn:link href="${browser.filePath}" class="fa fa-globe btn btn-default"
                          title="Open in a separate view" target="_blank"><span
                        class="label">Open</span></cpn:link>
            </div>
        </div>
        <div class="detail-content">
            <div class="image-frame ${browser.imageCSS}">
                <div class="image-background"
                     style="background-image:url(${cpn:unmappedUrl(slingRequest,cpn:cpm('composum/nodes/commons/images/image-background.png'))})">
                    <img src=""/>
                </div>
            </div>
        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
