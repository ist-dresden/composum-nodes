<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="composum-nodes-system-felix form-inline">
    <div class="composum-nodes-system-felix_tags widget text-field-widget form-group">
        <label>${cpn:i18n(slingRequest,'Tags')}</label>
        <div class="input-group">
            <input type="text" class="form-control" placeholder="e.g. status,general">
            <span class="input-group-btn"><button
                    type="button" class="composum-nodes-system-felix_refresh btn btn-default fa fa-refresh"
                    title="${cpn:i18n(slingRequest,'Refresh status')}"></button></span>
        </div>
    </div>
    <div class="composum-nodes-system-felix_content"></div>
</div>
