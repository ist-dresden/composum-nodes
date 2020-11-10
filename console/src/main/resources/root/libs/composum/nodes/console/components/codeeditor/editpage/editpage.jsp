<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="editor" type="com.composum.sling.nodes.components.codeeditor.CodeEditor" scope="request">
    <html data-context-path="${slingRequest.contextPath}">
    <head>
        <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1, user-scalable=no"/>
        <title>Edit - ${editor.path}</title>
        <cpn:clientlib type="css" category="composum.edit.codeeditor"/>
    </head>
    <body id="editor" class="composum-nodes-components-codeeditor_page"
          data-path="${editor.contentPath}" data-type="${editor.textType}">
    <sling:call script="/libs/composum/nodes/commons/components/dialogs/alert.jsp"/>
    <sling:call script="/libs/composum/nodes/console/dialogs/user-status.jsp"/>
    <sling:include resourceType="composum/nodes/console/components/codeeditor"/>
    <cpn:clientlib type="js" category="composum.edit.codeeditor"/>
    </body>
    </html>
</cpn:component>
