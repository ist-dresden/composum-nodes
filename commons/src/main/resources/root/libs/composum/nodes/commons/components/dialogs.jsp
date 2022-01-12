<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<cpn:defineObjects />
<sling:call script="${composumBase}composum/nodes/commons/components/dialogs/path-select.jsp"/>
<sling:call script="${composumBase}composum/nodes/commons/components/dialogs/alert.jsp"/>
