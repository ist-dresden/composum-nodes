<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="console" type="com.composum.sling.nodes.console.ConsoleModel" scope="request">
    <div class="content-wrapper">
        <iframe src="${cpn:url(slingRequest,console.contentSrc)}" width="100%" height="100%"
                sandbox="allow-same-origin allow-scripts allow-modals allow-popups allow-forms allow-downloads"></iframe>
    </div>
</cpn:component>
