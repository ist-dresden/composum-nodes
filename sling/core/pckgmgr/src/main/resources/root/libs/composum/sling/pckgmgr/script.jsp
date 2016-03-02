<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<sling:call script="/libs/composum/sling/console/page/script.jsp"/>
<cpn:clientlib path="composum/sling/pckgmgr/js/dialogs.js"/>
<cpn:clientlib path="composum/sling/pckgmgr/group/js/group.js"/>
<cpn:clientlib path="composum/sling/pckgmgr/jcrpckg/js/jcrpckg.js"/>
<cpn:clientlib path="composum/sling/pckgmgr/js/pckgmgr.js"/>
