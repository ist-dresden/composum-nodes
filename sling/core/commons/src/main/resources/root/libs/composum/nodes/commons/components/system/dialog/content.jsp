<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.ResourceModel">
    <form class="composum-nodes-system-dialog_form widget-form">
        <ul class="composum-commons-form-tab-nav nav nav-tabs" role="tablist">
        </ul>
        <div class="composum-nodes-system-dialog_panels tab-content composum-commons-form-tabbed">
            <div id="${model.domId}_felix" data-key="felix"
                 data-label="${cpn:i18n(slingRequest,'Felix Health Check')}"
                 class="composum-commons-form-tab-panel tab-pane" role="tabpanel">
                <sling:include resourceType="composum/nodes/commons/components/system/felix"/>
            </div>
        </div>
    </form>
</cpn:component>
