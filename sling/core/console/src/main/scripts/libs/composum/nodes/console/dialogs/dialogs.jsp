<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:call script="user-status.jsp"/>
<sling:call script="/libs/composum/nodes/commons/components/dialogs.jsp"/>
<sling:call script="approval.jsp"/>
<sling:call script="purge-audit.jsp"/>
<sling:call script="node-mixins.jsp"/>
<sling:call script="node-move.jsp"/>
<sling:call script="node-rename.jsp"/>
<sling:call script="node-copy.jsp"/>
<sling:call script="node-create.jsp"/>
<sling:call script="node-delete.jsp"/>
<sling:call script="node-upload.jsp"/>
<sling:call script="file-update.jsp"/>
