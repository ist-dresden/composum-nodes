<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:call script="dialogs/access-policy-entry.jsp"/>
<sling:call script="dialogs/node-mixins.jsp"/>
<sling:call script="dialogs/node-move.jsp"/>
<sling:call script="dialogs/node-rename.jsp"/>
<sling:call script="dialogs/node-copy.jsp"/>
<sling:call script="dialogs/node-create.jsp"/>
<sling:call script="dialogs/node-delete.jsp"/>
<sling:call script="dialogs/node-upload.jsp"/>
<sling:call script="dialogs/file-update.jsp"/>
<sling:call script="dialogs/version-add-label.jsp"/>
<sling:call script="dialogs/version-delete-label.jsp"/>
<sling:call script="dialogs/version-delete.jsp"/>
<sling:include resourceType="composum/nodes/console/components/codeeditor/editdialog"/>
<sling:call script="dialogs/property.jsp"/>
<sling:call script="/libs/composum/nodes/console/page/dialogs.jsp"/>
