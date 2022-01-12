<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<form class="composum-nodes-references-options_form widget-form">
    <input name="_charset_" type="hidden" value="UTF-8"/>
    <div class="row">
        <div class="col col-xs-8">
            <div class="searchRoot form-group">
                <label class="control-label">${cpn:i18n(slingRequest,'Search Root')}</label>
                <div class="input-group widget path-widget">
                    <input name="root" type="text" class="form-control"
                           placeholder="${cpn:i18n(slingRequest,'Select path...')}"/>
                    <span class="input-group-btn"><button
                            class="select btn btn-default" type="button"
                            title="${cpn:i18n(slingRequest,'Select Repository Path')}">...</button></span>
                </div>
            </div>
        </div>
        <div class="col col-xs-4">
            <div class="form-checkbox">
                <div class="absPath checkbox widget checkbox-widget">
                    <label><input name="abs" type="checkbox"/>${cpn:i18n(slingRequest,'absolute references')}</label>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col col-xs-2">
            <div class="form-checkbox-col">
                <div class="findRichText checkbox widget checkbox-widget">
                    <label><input name="rich" type="checkbox"/>${cpn:i18n(slingRequest,'rich text')}</label>
                </div>
            </div>
        </div>
        <div class="col col-xs-4">
            <div class="form-checkbox-col">
                <div class="useTextSearch checkbox widget checkbox-widget">
                    <label><input name="text" type="checkbox"/>${cpn:i18n(slingRequest,'use text search')}</label>
                </div>
            </div>
        </div>
        <div class="col col-xs-2">
            <div class="form-checkbox-col">
                <div class="includeChildren checkbox widget checkbox-widget">
                    <label><input name="ic" type="checkbox"/>${cpn:i18n(slingRequest,'include children')}</label>
                </div>
            </div>
        </div>
        <div class="col col-xs-4">
            <div class="form-checkbox-col">
                <div class="childrenOnly checkbox widget checkbox-widget">
                    <label><input name="co" type="checkbox"/>${cpn:i18n(slingRequest,'children only')}</label>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col col-xs-8">
            <div class="basePath form-group">
                <label class="control-label">${cpn:i18n(slingRequest,'Base Path')}<span
                        class="widget-hint">${cpn:i18n(slingRequest,'a given base path will be ignored to find relative references')}</span></label>
                <div class="input-group widget path-widget">
                    <input name="base" type="text" class="form-control"
                           placeholder="${cpn:i18n(slingRequest,'Select path...')}"/>
                    <span class="input-group-btn"><button
                            class="select btn btn-default" type="button"
                            title="${cpn:i18n(slingRequest,'Select Repository Path')}">...</button></span>
                </div>
            </div>
        </div>
        <div class="col col-xs-4">
            <div class="form-checkbox">
                <div class="relPath checkbox widget checkbox-widget">
                    <label><input name="rel" type="checkbox"/>${cpn:i18n(slingRequest,'relative references')}</label>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col col-xs-8">
            <div class="primaryType form-group">
                <label class="control-label">${cpn:i18n(slingRequest,'Primary Type')}</label>
                <input name="pt" class="widget text-widget form-control" type="text">
            </div>
        </div>
        <div class="col col-xs-4">
            <div class="resourceName form-group">
                <label class="control-label">${cpn:i18n(slingRequest,'Resource Name')}</label>
                <input name="rn" class="widget text-widget form-control" type="text">
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col col-xs-12">
            <div class="resourceType form-group">
                <label class="control-label">${cpn:i18n(slingRequest,'Resource Type')}</label>
                <input name="rt" class="widget text-widget form-control" type="text">
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col col-xs-8">
            <div class="contentPath form-group">
                <label class="control-label">${cpn:i18n(slingRequest,'Content Path')}</label>
                <input name="cp" class="widget text-widget form-control" type="text">
            </div>
        </div>
        <div class="col col-xs-4">
            <div class="propertyName form-group">
                <label class="control-label">${cpn:i18n(slingRequest,'Property Name')}</label>
                <input name="pn" class="widget text-widget form-control" type="text">
            </div>
        </div>
    </div>
</form>
