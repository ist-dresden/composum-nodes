<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div id="alert-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content form-panel">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="${cpn:i18n(slingRequest,'Close')}"><span
                        aria-hidden="true">&times;</span></button>
                <h4 class="modal-title">${cpn:i18n(slingRequest,'Alert')}</h4>
            </div>
            <div class="modal-body">
                <div class="messages">
                    <div class="hidden">
                        <div class="panel-heading"></div>
                        <div class="panel-body"></div>
                    </div>
                </div>
            </div>
            <div class="modal-footer buttons">
                <button type="button" class="btn btn-primary"
                        data-dismiss="modal">${cpn:i18n(slingRequest,'Close')}</button>
            </div>
        </div>
    </div>
</div>
