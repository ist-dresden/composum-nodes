<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div class="xml detail-panel">
    <div class="embedded frame-container detail-content">
      <iframe src="${browser.current.pathEncoded}.xml" width="100%" height="100%"></iframe>
    </div>
  </div>
</cpn:component>