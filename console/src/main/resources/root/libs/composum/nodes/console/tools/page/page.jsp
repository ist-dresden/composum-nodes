<%@page pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/><cpn:component var="console" type="com.composum.sling.nodes.console.ConsoleModel" scope="request">
<html data-context-path="${slingRequest.contextPath}">
<head>
    <sling:call script="/libs/composum/nodes/console/page/head-meta.jsp"/>
    <sling:call script="head-clientlibs.jsp"/>
</head>
<body id="${console.id}" class="${console.id} tools-console console" data-tools="${console.dataSet}">
<div id="ui">
    <sling:include resourceType="/libs/composum/nodes/console/dialogs" replaceSelectors="minimal"/>
    <sling:include resourceType="composum/nodes/console/components/navbar"/>
    <sling:call script="content.jsp"/>
</div>
<sling:call script="body-clientlibs.jsp"/>
<sling:include resourceType="composum/nodes/console/components/tryLogin"/>
</body>
</html>
</cpn:component>
