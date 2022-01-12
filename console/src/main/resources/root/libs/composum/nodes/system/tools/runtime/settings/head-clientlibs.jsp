<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:clientlib type="link" category="composum.nodes.console.tools"/>
<cpn:clientlib type="css" category="composum.nodes.console.tools"/>
<link rel="stylesheet"
      href="${cpn:url(slingRequest,cpn:cpm('composum/nodes/system/tools/runtime/settings/css/settings.css'))}">
