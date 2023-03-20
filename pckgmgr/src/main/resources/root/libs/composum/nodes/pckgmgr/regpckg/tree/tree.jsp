<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean" scope="request">
    <c:set var="writeAllowed" value="${pckgmgr.writeAllowed}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="nodes-pckgmgr-regpckg-tree">
        <div class="tree-panel">
            <div id="regpckg-tree" data-selected="${pckgmgr.path}">
            </div>
        </div>
        <div class="tree-actions action-bar btn-toolbar" role="toolbar">
            <div class="align-left">
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="refresh glyphicon-refresh glyphicon btn btn-default"
                            title="${cpn:i18n(slingRequest,'Refresh tree view')}"><span
                            class="label">${cpn:i18n(slingRequest,'Refresh')}</span></button>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="delete fa fa-minus btn btn-default"${writeDisabled}
                            title="${cpn:i18n(slingRequest,'Delete selected package')}"><span
                            class="label">${cpn:i18n(slingRequest,'Delete')}</span></button>
                    <button type="button" class="cleanup fa fa-recycle btn btn-default"${writeDisabled}
                            title="Cleanup obsolete package versions"><span class="label">Cleanup</span></button>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="upload fa fa-upload btn btn-default"${writeDisabled}
                            title="${cpn:i18n(slingRequest,'Upload a new package')}"><span
                            class="label">${cpn:i18n(slingRequest,'Upload')}</span></button>
                    <a type="button" class="download fa fa-download btn btn-default"
                       title="${cpn:i18n(slingRequest,'Download selected package')}"><span
                            class="label">${cpn:i18n(slingRequest,'Download')}</span></a>
                </div>
            </div>
            <div class="align-right">
                <div class="regpckg-mode-merged tree-actions_checkbox">
                    <label><input type="checkbox">${cpn:i18n(slingRequest,'merged')}</label>
                </div>
            </div>
        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
