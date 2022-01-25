<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:clientlib type="js" category="composum.nodes.console.tools"/>
<script src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/system/tools/osgi/bundles/js/bundles.js'))}"></script>
