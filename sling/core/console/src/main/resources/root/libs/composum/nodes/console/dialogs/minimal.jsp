<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:call script="user-status.jsp"/>
<sling:call script="/libs/composum/nodes/commons/components/dialogs.jsp"/>
<sling:call script="approval.jsp"/>
