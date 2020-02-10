<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="composum-nodes-system-dialog dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content form-panel default">
            <div class="modal-header">
                <button type="button" class="modal-dialog_close fa fa-close" data-dismiss="modal"
                        title="${cpn:i18n(slingRequest,'Close')}"
                        aria-label="${cpn:i18n(slingRequest,'Close')}"></button>
                <h4 class="modal-title">${cpn:i18n(slingRequest,'System Status')}</h4>
            </div>
            <div class="composum-nodes-system-dialog_body modal-body">
                <div class="composum-nodes-system-dialog_messages messages">
                    <div class="panel panel-warning hidden">
                        <div class="panel-heading"></div>
                        <div class="panel-body hidden"></div>
                    </div>
                </div>
                <input name="_charset_" type="hidden" value="UTF-8"/>
                <div class="composum-nodes-system-dialog_content">
                    <sling:call script="content.jsp"/>
                </div>
            </div>
            <div class="composum-nodes-system-dialog_footer modal-footer buttons">
                <button type="button" class="btn btn-default"
                        data-dismiss="modal">${cpn:i18n(slingRequest,'Close')}</button>
            </div>
        </div>
    </div>
</div>
