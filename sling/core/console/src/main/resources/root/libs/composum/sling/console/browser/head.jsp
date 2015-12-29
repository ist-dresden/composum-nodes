<%@page session="false" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<head>
<sling:call script="/libs/composum/sling/console/page/head.jsp"/>
  <!-- browser style -->
  <cpn:clientlib path="composum/sling/console/browser/css/nodes.css"/>
  <cpn:clientlib path="composum/sling/console/browser/css/browser.css"/>
  <!-- browser components -->
  <cpn:clientlib path="composum/sling/console/browser/components/favorites/favorites.css"/>
  <%--
  <!-- browser theme - experimental -->
  <cpn:clientlib path="/libs/composum/sling/console/page/css/theme-dark.css"/>
  --%>
</head>
