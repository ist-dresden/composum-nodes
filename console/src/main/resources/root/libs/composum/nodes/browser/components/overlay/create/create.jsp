<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div id="alert-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content form-panel">
            <cpn:form method="POST" classes="widget-form default"
                      action="/bin/cpm/nodes/components.createOverlay.json${cpn:path(slingRequest.requestPathInfo.suffix)}">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal"
                            aria-label="${cpn:i18n(slingRequest,'Cancel')}"><span
                            aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">${cpn:i18n(slingRequest,'Create an Overlay / Base')}</h4>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <div class="hidden">
                            <div class="panel-heading"></div>
                            <div class="panel-body"></div>
                        </div>
                    </div>
                    <div class="form-group">
                        <cpn:text class="text" i18n="true"
                                  type="rich">Do you want to create an overlay / base to the current component?</cpn:text>
                    </div>
                    <div class="form-group">
                        <label class="control-label">${cpn:i18n(slingRequest,'Overlay / Base Path')}</label>
                        <input name="path" class="form-control" type="text" disabled="disabled"
                               value="${cpn:value(slingRequest.requestPathInfo.suffix)}"/>
                    </div>
                </div>
                <div class="modal-footer buttons">
                    <button type="button" class="btn btn-default"
                            data-dismiss="modal">${cpn:i18n(slingRequest,'Cancel')}</button>
                    <button type="submit" class="btn btn-primary">${cpn:i18n(slingRequest,'Create')}</button>
                </div>
            </cpn:form>
        </div>
    </div>
</div>
