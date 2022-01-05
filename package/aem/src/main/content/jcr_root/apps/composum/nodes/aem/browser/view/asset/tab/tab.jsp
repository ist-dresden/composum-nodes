<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="genericView" type="com.composum.sling.nodes.browser.GenericView" scope="request">
    <div class="${genericView.id} detail-panel" data-id="${genericView.id}" data-file="${genericView.path}"
         data-path="${genericView.unmappedUrl}" data-mapped="${genericView.mappedUrl}">
        <sling:call script="body.jsp"/>
    </div>
</cpn:component>
