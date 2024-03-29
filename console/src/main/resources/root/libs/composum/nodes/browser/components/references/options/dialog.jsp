<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="composum-nodes-references-options dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content form-panel default">
            <div class="modal-header">
                <button type="button" class="modal-dialog_close fa fa-close" data-dismiss="modal"
                        title="${cpn:i18n(slingRequest,'Close')}"
                        aria-label="${cpn:i18n(slingRequest,'Close')}"></button>
                <h4 class="modal-title">${cpn:i18n(slingRequest,'References Options')}</h4>
            </div>
            <div class="composum-nodes-references-options_body modal-body">
                <div class="composum-nodes-references-options_messages messages">
                    <div class="panel panel-warning hidden">
                        <div class="panel-heading"></div>
                        <div class="panel-body hidden"></div>
                    </div>
                </div>
                <div class="composum-nodes-references-options_content">
                    <sling:include replaceSelectors=""/>
                </div>
            </div>
            <div class="composum-nodes-references-options_footer modal-footer buttons">
                <button type="button" class="cancel btn btn-default"
                        data-dismiss="modal">${cpn:i18n(slingRequest,'Cancel')}</button>
                <button type="submit" class="apply btn btn-primary">${cpn:i18n(slingRequest,'Apply')}</button>
            </div>
        </div>
    </div>
</div>
