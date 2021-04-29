<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="blank detail-panel">
    <div class="detail-tabs action-bar btn-toolbar" role="toolbar">
        <div class="btn-group btn-group-sm" role="group">
            <a class="general fa fa-folder-o btn btn-default" href="#general" data-group="general"
               title="${cpn:i18n(slingRequest,'Folder')}">
                <div class="title">${cpn:i18n(slingRequest,'Folder')}</div>
            </a>
        </div>
    </div>
</div>
<div class="detail-content">
</div>
