<%@page pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<cpn:defineObjects/>
<cpn:component var="console" type="com.composum.sling.nodes.console.ConsoleModel" scope="request">
<html data-context-path="${slingRequest.contextPath}" data-composum-base="${composumBase}">
<head>
    <sling:call script="${composumBase}composum/nodes/console/page/head-meta.jsp"/>
    <sling:call script="head-clientlibs.jsp"/>
</head>
<body id="${console.id}" class="${console.id} tools-console console standalone" data-tools="${console.dataSet}">
<div id="ui">
    <sling:call script="content.jsp"/>
</div>
<sling:call script="body-clientlibs.jsp"/>
</body>
</html>
</cpn:component>
