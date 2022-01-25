<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model" type="com.composum.sling.nodes.components.MergedModel" scope="request">
    <c:set var="writeAllowed" value="${model.permissible['nodes/repository/properties']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="merged-list">
        <c:forEach items="${model.mergedResources}" var="resource" varStatus="loop">
            <div class="merged-list_resource merged-resource-${loop.index} panel panel-default"
                 data-path="${cpn:path(resource.path)}">
                <div class="panel-heading" role="tab" id="merged-tab-${loop.index}">
                    <h4 class="panel-title">
                        <a href="#merged-panel-${loop.index}" aria-controls="merged-panel-${loop.index}"
                           aria-expanded="true" data-toggle="collapse" data-path="${resource.path}"
                           role="button"><span class="panel-heading-label">${cpn:text(resource.path)}</span>
                            <div class="table-toolbar btn-toolbar">
                                <div class="btn-group btn-group-sm btn-group-edit" role="group">
                                    <button class="add fa fa-plus btn btn-default"${writeDisabled}
                                            title="${cpn:i18n(slingRequest,'Add new property')}"><span
                                            class="label">${cpn:i18n(slingRequest,'Add')}</span>
                                    </button>
                                    <button class="remove fa fa-minus btn btn-default"${writeDisabled}
                                            title="${cpn:i18n(slingRequest,'Remove selected properties')}"><span
                                            class="label">${cpn:i18n(slingRequest,'Remove')}</span></button>
                                </div>
                                <div class="btn-group btn-group-sm btn-group-edit" role="group">
                                    <button class="copy fa fa-copy btn btn-default"${writeDisabled}
                                            title="${cpn:i18n(slingRequest,'Copy selected properties')}"><span
                                            class="label">${cpn:i18n(slingRequest,'Copy')}</span>
                                    </button>
                                    <button class="paste fa fa-paste btn btn-default"${writeDisabled}
                                            title="${cpn:i18n(slingRequest,'Paste copied properties')}"><span
                                            class="label">${cpn:i18n(slingRequest,'Paste')}</span>
                                    </button>
                                </div>
                                <div class="btn-group btn-group-sm btn-group-edit" role="group">
                                    <button type="button" class="reload fa fa-refresh btn btn-default"
                                            title="${cpn:i18n(slingRequest,'Reload')}"><span
                                            class="label">${cpn:i18n(slingRequest,'Reload')}</span>
                                    </button>
                                </div>
                                <div class="btn-group btn-group-sm" role="group">
                                    <cpn:link href="/bin/browser.html${cpn:path(resource.path)}"
                                              class="target-link fa fa-share btn btn-default"
                                              title="${cpn:i18n(slingRequest,'Open')}">
                                        <span class="label">${cpn:i18n(slingRequest,'Open')}</span>
                                    </cpn:link>
                                </div>
                            </div>
                        </a>
                    </h4>
                </div>
                <div id="merged-panel-${loop.index}" class="merged-list_resource-properties panel-collapse collapse in"
                     role="tabpanel" aria-labelledby="merged-tab-${loop.index}">
                    <div class="panel-body merged-list_properties table-container"
                         data-path="${cpn:path(resource.path)}" data-permission="${writeAllowed?'write':'read'}">
                        <table id="browser-view-property-table" class="property-table">
                        </table>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
