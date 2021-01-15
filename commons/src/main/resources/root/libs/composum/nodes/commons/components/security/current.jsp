<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.ResourceModel">
    <div class="composum-nodes-security-config">
        <div class="composum-nodes-security-config_top">
            <cpn:form class="composum-nodes-security-config_scripts widget-form"
                      method="POST" action="/bin/cpm/nodes/setup.run.txt">
                <div class="composum-nodes-security-config_scripts-wrapper">
                    <div class="composum-nodes-security-config_current-script"
                         data-path="${slingRequest.requestPathInfo.suffix}"></div>
                </div>
                <div class="composum-nodes-security-config_current-actions">
                    <input type="hidden" name="script" value="${slingRequest.requestPathInfo.suffix}"/>
                    <div class="composum-nodes-security-config_actions-buttons">
                        <button class="composum-nodes-security-config_actions-run btn btn-primary"
                                type="submit">${cpn:i18n(slingRequest,'Run Current...')}</button>
                    </div>
                </div>
            </cpn:form>
        </div>
        <div class="composum-nodes-security-config_bottom">
        </div>
    </div>
</cpn:component>
