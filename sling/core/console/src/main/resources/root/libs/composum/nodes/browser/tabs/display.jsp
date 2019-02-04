<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div class="display detail-panel" data-path="${browser.currentPathUrl}" data-mapped="${browser.mappedUrl}">
    <div class="display-toolbar detail-toolbar">
      <div class="btn-group btn-group-sm" role="group">
        <span class="resolver fa fa-external-link btn btn-default" title="Resolver Mapping ON/OFF"></span>
      </div>
      <div class="prefix btn-group btn-group-sm toolbar-input-field" role="group">
        <div class="input-group input-group-sm">
          <span class="input-group-addon" title="Path prefix">/_/</span>
          <input type="text" class="form-control" placeholder="Path prefix">
        </div>
      </div>
      <div class="selectors btn-group btn-group-sm toolbar-input-field" role="group">
        <div class="input-group input-group-sm">
          <span class="input-group-addon" title="Sling selectors">.x.</span>
          <input type="text" class="form-control" placeholder="Sling selectors">
        </div>
      </div>
      <div class="extension btn-group btn-group-sm toolbar-input-field" role="group">
        <div class="input-group input-group-sm">
          <span class="input-group-addon" title="Extension">.xt</span>
          <input type="text" class="form-control" placeholder="Ext.">
        </div>
      </div>
      <div class="suffix btn-group btn-group-sm toolbar-input-field" role="group">
        <div class="input-group input-group-sm">
          <span class="input-group-addon" title="Sling suffix">/..</span>
          <input type="text" class="form-control" placeholder="Sling suffix">
        </div>
      </div>
      <div class="parameters btn-group btn-group-sm toolbar-input-field" role="group">
        <div class="input-group input-group-sm">
          <span class="fa fa-question input-group-addon" title="URL parameter"></span>
          <input type="text" class="form-control" placeholder="URL parameter">
        </div>
      </div>
      <div class="webview btn-group btn-group-sm" role="group">
        <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span></button>
        <button type="button" class="open fa fa-globe btn btn-default" title="Open in a separate view"><span class="label">Open</span></button>
      </div>
    </div>
    <div class="embedded frame-container detail-content">
      <iframe src="" width="100%" height="100%"></iframe>
    </div>
  </div>
</cpn:component>