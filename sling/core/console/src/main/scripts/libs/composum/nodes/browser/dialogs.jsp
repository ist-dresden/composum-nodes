<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:include resourceType="/libs/composum/nodes/console/dialogs"/>
<sling:call script="dialogs/access-policy-entry.jsp"/>
<sling:call script="dialogs/version-add-label.jsp"/>
<sling:call script="dialogs/version-delete-label.jsp"/>
<sling:call script="dialogs/version-delete.jsp"/>
<sling:include resourceType="composum/nodes/console/components/codeeditor/editdialog"/>
<sling:call script="dialogs/property.jsp"/>
