<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="merged-toolbar detail-toolbar flex-toolbar">
    <div class="btn-group btn-group-sm" role="group">
        <button type="button" class="refresh fa fa-refresh btn btn-default"
                title="${cpn:i18n(slingRequest,'Reload')}"><span
                class="label">${cpn:i18n(slingRequest,'Reload')}</span>
        </button>
    </div>
</div>
