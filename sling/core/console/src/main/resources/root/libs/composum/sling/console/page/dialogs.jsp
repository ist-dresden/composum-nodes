<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:call script="/libs/composum/sling/console/page/dialogs/user-status.jsp"/>
<sling:call script="/libs/composum/sling/commons/components/dialogs.jsp"/>
<sling:call script="/libs/composum/sling/console/page/dialogs/alert.jsp"/>
<sling:call script="/libs/composum/sling/console/page/dialogs/approval.jsp"/>
<sling:call script="/libs/composum/sling/console/page/dialogs/purge-audit.jsp"/>
