<%@page session="false" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<cpn:defineObjects/>
<head>
<sling:call script="${composumBase}composum/nodes/console/page/head-meta.jsp"/>
<cpn:clientlib type="link" category="composum.nodes.console.default"/>
<cpn:clientlib type="css" category="composum.nodes.console.browser"/>
</head>
