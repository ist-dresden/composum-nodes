<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
  <meta charset="UTF-8"/>
  <!-- disable caching -->
  <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate"/>
  <meta http-equiv="Pragma" content="no-cache"/>
  <meta http-equiv="Expires" content="0"/>
  <!-- responsive viewport -->
  <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1, user-scalable=no"/>
  <!-- full screen application mode -->
  <meta name="mobile-web-app-capable" content="yes"/>
  <meta name="apple-mobile-web-app-capable" content="yes"/>
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent"/>
  <!-- switch of auto link for phone numbers -->
  <meta name="format-detection" content="telephone=no"/>
<cpn:clientlib type="link" category="composum.nodes.console.default"/>
<cpn:clientlib type="css" category="composum.nodes.console.default"/>
