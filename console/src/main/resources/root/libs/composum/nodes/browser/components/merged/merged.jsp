<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model" type="com.composum.sling.nodes.components.MergedModel" scope="request">
    <div class="merged detail-panel" data-path="${cpn:path(model.path)}">
        <sling:call script="toolbar.jsp"/>
        <div class="detail-content">
        </div>
    </div>
</cpn:component>
