<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:call script="dialogs/pckg-create.jsp"/>
<sling:call script="dialogs/pckg-delete.jsp"/>
<sling:call script="dialogs/pckg-upload.jsp"/>
<sling:call script="/libs/composum/sling/console/page/dialogs.jsp"/>
