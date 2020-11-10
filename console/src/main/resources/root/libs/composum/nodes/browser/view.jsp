<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div class="detail-view">
    <sling:call script="/libs/composum/nodes/browser/breadcrumbs.jsp" />
    <sling:call script="/libs/composum/nodes/browser/views/${browser.viewType}.jsp" />
  </div>
</cpn:component>