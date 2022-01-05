<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="references-toolbar detail-toolbar flex-toolbar">
    <div class="searchRoot btn-group btn-group-sm toolbar-input-field" role="group">
        <div class="input-group input-group-sm widget path-widget" title="${cpn:i18n(slingRequest,'Search Root')}">
            <span class="input-group-addon"><i class="fa fa-folder-o"></i></span>
            <input name="root" type="text" class="form-control" placeholder="${cpn:i18n(slingRequest,'Search Root')}">
            <span class="select input-group-addon" title="${cpn:i18n(slingRequest,'Select Repository Path')}">...</span>
        </div>
    </div>
    <!--
    <div class="basePath btn-group btn-group-sm toolbar-input-field" role="group">
        <div class="input-group input-group-sm widget path-widget" title="${cpn:i18n(slingRequest,'Base Path')}">
            <span class="input-group-addon">/_/</span>
            <input name="base" type="text" class="form-control" placeholder="${cpn:i18n(slingRequest,'Base Path')}">
            <span class="select input-group-addon" title="${cpn:i18n(slingRequest,'Select Repository Path')}">...</span>
        </div>
    </div>
    -->
    <div class="checkbox-group" role="group">
        <!--
        <label class="checkbox-control" title="${cpn:i18n(slingRequest,'absolute paths')}"><input
                name="abs" class="absPath checkbox-field" type="checkbox"/><span
                class="checkbox-label">/abs</span></label>
        <label class="checkbox-control" title="${cpn:i18n(slingRequest,'relative paths')}"><input
                name="rel" class="relPath checkbox-field" type="checkbox"/><span
                class="checkbox-label">rel/</span></label>
        -->
        <label class="checkbox-control" title="${cpn:i18n(slingRequest,'include children')}"><input
                name="ic" class="includeChildren checkbox-field" type="checkbox"/><span
                class="checkbox-label">/...</span></label>
    </div>
    <div class="checkbox-group" role="group">
        <label class="checkbox-control" title="${cpn:i18n(slingRequest,'use text search')}"><input
                name="text" class="useTextSearch checkbox-field" type="checkbox"/><span
                class="checkbox-label">txt</span></label>
        <label class="checkbox-control" title="${cpn:i18n(slingRequest,'search in rich text')}"><input
                name="rich" class="findRichText checkbox-field" type="checkbox"/><span
                class="checkbox-label">&lt;p&gt;</span></label>
    </div>
    <div class="btn-group btn-group-sm" role="group">
        <button type="button" class="options fa fa-list-ul btn btn-default"
                title="${cpn:i18n(slingRequest,'Search Options')}"><span
                class="label">${cpn:i18n(slingRequest,'Options')}</span>
        </button>
    </div>
    <div class="btn-group btn-group-sm" role="group">
        <button type="button" class="refresh fa fa-refresh btn btn-default"
                title="${cpn:i18n(slingRequest,'Reload')}"><span
                class="label">${cpn:i18n(slingRequest,'Reload')}</span>
        </button>
    </div>
</div>
