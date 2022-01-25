<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<cpn:defineObjects/>
<div class="tools-runtime-files_view-logo">
    <a href="${cpn:url(slingRequest,cpn:cpm('composum/nodes/console/content'))}">
        <cpn:image src="${composumBase}composum/nodes/console/page/images/composum-nodes.svg" alt=""/>
    </a>
</div>