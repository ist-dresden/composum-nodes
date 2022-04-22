<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="genericView" type="com.composum.sling.nodes.browser.GenericView" scope="request">
    <div class="${genericView.id}-detail detail-panel" data-id="${genericView.id}"
         data-type="${genericView.viewType}" data-file="${genericView.fileResource.path}"
         data-path="${cpn:unmappedUrl(slingRequest,genericView.fileResource.path)}"
         data-mapped="${cpn:mappedUrl(slingRequest,genericView.fileResource.path)}">
        <sling:call script="body.jsp"/>
    </div>
</cpn:component>
