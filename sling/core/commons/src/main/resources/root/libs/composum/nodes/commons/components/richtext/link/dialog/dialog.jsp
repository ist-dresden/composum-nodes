<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="composum-widgets-richtext_link-dialog_url form-group">
    <label class="control-label">${cpn:i18n(slingRequest,'Link (Content Path or external URL)')}</label>
    <div class="link input-group widget path-widget" data-rules="required">
        <input name="url" class="form-control" type="text" autofocus/>
        <span class="input-group-btn">
            <button class="select btn btn-default" type="button"
                    title="${cpn:i18n(slingRequest,'Select Repository Path')}">...</button></span>
    </div>
</div>
<div class="composum-widgets-richtext_link-dialog_text form-group">
    <label class="control-label">${cpn:i18n(slingRequest,'Link Text')}</label>
    <input name="text" class="widget text-field-widget form-control" type="text"/>
</div>
<div class="composum-widgets-richtext_link-dialog_title form-group">
    <label class="control-label">${cpn:i18n(slingRequest,'Link Title')}</label>
    <input name="title" class="widget text-field-widget form-control" type="text"/>
</div>
<div class="composum-widgets-richtext_link-dialog_target form-group">
    <label class="control-label">${cpn:i18n(slingRequest,'Link Target')}</label>
    <input name="target" class="widget text-field-widget form-control" type="text"/>
</div>
