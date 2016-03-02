<%@page session="false" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@ taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<head>
<sling:call script="/libs/composum/sling/console/page/head.jsp"/>
  <!-- package manager styles -->
  <cpn:clientlib path="composum/sling/pckgmgr/css/pckgmgr.css" />
  <cpn:clientlib path="composum/sling/pckgmgr/group/css/group.css" />
  <cpn:clientlib path="composum/sling/pckgmgr/jcrpckg/css/jcrpckg.css" />
</head>
