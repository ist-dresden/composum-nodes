<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
<body id="overview" class="console">
<div id="ui">
    <sling:include resourceType="/libs/composum/nodes/console/dialogs" replaceSelectors="minimal"/>
    <sling:include resourceType="composum/nodes/console/components/navbar"/>
    <sling:call script="content.jsp"/>
</div>
<sling:call script="script.jsp"/>
<sling:include resourceType="composum/nodes/console/components/tryLogin"/>
</body>
