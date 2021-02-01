<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:form class="composum-nodes-security-config_scripts widget-form"
          method="POST" action="/bin/cpm/nodes/setup.run.txt">
    <div class="composum-nodes-security-config_scripts-wrapper">
        <sling:call script="scripts.jsp"/>
    </div>
    <div class="composum-nodes-security-config_scripts-actions">
            <span class="composum-nodes-security-config_actions-all"><input
                    class="composum-nodes-security-config_actions-all-checkbox"
                    type="checkbox"/>${cpn:i18n(slingRequest,'select / deselect all')}</span>
        <div class="composum-nodes-security-config_actions-buttons">
            <button class="composum-nodes-security-config_actions-run btn btn-primary"
                    type="submit">${cpn:i18n(slingRequest,'Run Selected...')}</button>
        </div>
    </div>
</cpn:form>
