<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<sling:call script="/libs/composum/nodes/console/page/script.jsp"/>
<cpn:clientlib type="js" category="composum.nodes.console.pckgmgr"/>
