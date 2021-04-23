<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="preferences detail-panel full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="add fa fa-plus btn btn-default" title="${cpn:i18n(slingRequest,'Add new property')}">
                    <span class="label">${cpn:i18n(slingRequest,'Add')}</span>
                </button>
                <button class="remove fa fa-minus btn btn-default"
                        title="${cpn:i18n(slingRequest,'Remove selected properties')}"><span
                        class="label">${cpn:i18n(slingRequest,'Remove')}</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="copy fa fa-copy btn btn-default"
                        title="${cpn:i18n(slingRequest,'Copy selected properties')}"><span
                        class="label">${cpn:i18n(slingRequest,'Copy')}</span>
                </button>
                <button class="paste fa fa-paste btn btn-default"
                        title="${cpn:i18n(slingRequest,'Paste copied properties')}"><span
                        class="label">${cpn:i18n(slingRequest,'Paste')}</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default"
                        title="${cpn:i18n(slingRequest,'Reload')}"><span
                        class="label">${cpn:i18n(slingRequest,'Reload')}</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="delete fa fa-trash btn btn-default"
                        title="${cpn:i18n(slingRequest,'Delete Preferences')}"><span
                        class="label">${cpn:i18n(slingRequest,'Delete Prefrences')}</span>
                </button>
            </div>
        </div>
        <div class="table-container">
            <table id="user-view-preferences-table" class="property-table"
                   data-path="${model.path}/preferences"
                   data-toolbar=".detail-view .preferences .table-toolbar">
            </table>
        </div>
    </div>
</cpn:component>