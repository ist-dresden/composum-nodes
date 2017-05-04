<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div class="xml detail-panel" data-path="${browser.current.pathEncoded}">
    <div class="xml-toolbar detail-toolbar">
      <div class="btn-group btn-group-sm" role="group">
        <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span></button>
        <a href="" class="download fa fa-file-code-o btn btn-default" title="Download as XML file" target="_blank"><span class="label">Download</span></a>
      </div>
    </div>
    <div class="embedded frame-container detail-content">
      <iframe src="" width="100%" height="100%"></iframe>
    </div>
  </div>
</cpn:component>
