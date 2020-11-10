<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="composum-nodes-system">
    <div class="composum-nodes-system_users"><span
            class="label">${cpn:i18n(slingRequest,'Users')}:</span>&nbsp;<em></em></div>
    <div class="composum-nodes-system_health"><span
            class="label">${cpn:i18n(slingRequest,'Health')}:</span>&nbsp;<em></em>
    </div>
</div>
