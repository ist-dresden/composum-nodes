<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="detail-panel registry">
    <div class="detail-tabs action-bar btn-toolbar" role="toolbar">
        <div class="btn-group btn-group-sm" role="group">
            <a class="general" href="#general" data-group="general" title="Group">
                <div class="title">${cpn:i18n(slingRequest,'Packages')}</div>
            </a>
        </div>
    </div>
    <div class="detail-content">
    </div>
</div>
