<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="content-wrapper" data-suffix="${slingRequest.requestPathInfo.suffix}">
    <div class="tools-runtime-files_header">
        <sling:include replaceSelectors="${slingRequest.requestPathInfo.selectorString}logo"/>
        <h1 class="title">${cpn:i18n(slingRequest,'Runtime Files')}</h1>
    </div>
    <div class="tools-runtime-files_panel">
        <sling:include replaceSelectors="${slingRequest.requestPathInfo.selectorString}tree"/>
        <div class="tools-runtime-files_view-toolbar">
            <div class="btn-group">
                <button type="button" class="tail fa fa-play btn btn-default"
                        title="${cpn:i18n(slingRequest,'Tail on/off')}"></button>
                <button type="button" class="scroll btn btn-default"
                        title="${cpn:i18n(slingRequest,'Scroll to end')}"><i
                        class="fa fa-step-forward fa-rotate-90"></i></button>
                <button type="button" class="wrap btn btn-default"
                        title="${cpn:i18n(slingRequest,'Line wrap')}"><i
                        class="fa fa-level-down fa-rotate-90"></i></button>
            </div>
            <div class="input-group limit" title="${cpn:i18n(slingRequest,"Limit (last 'n' lines)")}">
                <input class="field form-control" type="text" size="6" placeholder="1000"/>
                <span class="action input-group-addon fa fa-step-backward"></span>
            </div>
            <div class="input-group filter" title="${cpn:i18n(slingRequest,'Filter (regular expression)')}">
                <span class="clear input-group-addon fa fa-times-circle"
                      title="${cpn:i18n(slingRequest,'clear filter')}"></span>
                <span class="problems input-group-addon fa fa-bolt"
                      title="${cpn:i18n(slingRequest,'errors and warnings filter... []')}"></span>
                <input class="pattern form-control" type="text" placeholder="${cpn:i18n(slingRequest,'filter')}"/>
                <input class="prepend form-control input-group-addon" type="text" placeholder="3"/>
                <input class="append form-control input-group-addon" type="text" placeholder="1"/>
                <span class="action input-group-addon fa fa-filter"></span>
            </div>
            <div class="btn-group">
                <button type="button" class="reload fa fa-refresh btn btn-default"
                        title="${cpn:i18n(slingRequest,'Reload')}"></button>
                <sling:include replaceSelectors="${slingRequest.requestPathInfo.selectorString}open"/>
            </div>
            <div class="btn-group">
                <button class="download fa fa-download btn btn-default"
                        title="${cpn:i18n(slingRequest,'Download')}"></button>
            </div>
        </div>
        <div class="tools-runtime-files_view-wrapper">
            <div class="tools-runtime-files_view-lock">
                <div><i class="fa fa-spinner fa-pulse"></i></div>
            </div>
            <div class="tools-runtime-files_view">
                <div></div>
            </div>
        </div>
    </div>
</div>